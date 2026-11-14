package com.ctu.freshgrocery.payment;

import com.ctu.freshgrocery.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SePayStore {
    private final JdbcTemplate jdbc;
    private final SePaySchema schema;

    public SePayPayment get(String id) {
        return jdbc.query("SELECT * FROM ThanhToanSePay WHERE PaymentId=?",SePayPayment::read,id).stream().findFirst()
                .orElseThrow(()->new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy thanh toán SePay."));
    }
    public Optional<SePayPayment> forOrder(int order, int customer) {
        return jdbc.query("SELECT * FROM ThanhToanSePay WHERE MaDH=? AND MaKH=?",SePayPayment::read,order,customer).stream().findFirst();
    }
    @Transactional
    public SePayPayment reserve(int customer, String address, String note, String key, String account, String bank, String holder) {
        var customers=jdbc.queryForList("SELECT MaKH FROM KhachHang WITH (UPDLOCK,ROWLOCK) WHERE MaKH=?",customer);
        if(customers.isEmpty())throw new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy khách hàng.");
        var existing=jdbc.query("""
            SELECT TOP 1 * FROM ThanhToanSePay WITH (UPDLOCK,HOLDLOCK)
            WHERE MaKH=? AND (IdempotencyKey=? OR Status IN ('PENDING','REVIEW'))
            ORDER BY CASE WHEN IdempotencyKey=? THEN 0 ELSE 1 END, CreatedAt DESC
            """,SePayPayment::read,customer,key,key);
        if(!existing.isEmpty())return existing.getFirst();
        var carts=jdbc.queryForList("SELECT MaGioHang FROM GioHang WITH (UPDLOCK,HOLDLOCK) WHERE MaKH=?",customer);
        if(carts.isEmpty())throw new AppException(HttpStatus.BAD_REQUEST,"Giỏ hàng rỗng.");
        int cart=((Number)carts.getFirst().get("MaGioHang")).intValue();
        var items=jdbc.queryForList("""
            SELECT sp.MaSP,ct.SoLuong,sp.Gia AS GiaGoc,
                ISNULL(km.GiaKhuyenMai,sp.Gia) AS DonGia,km.MaKM,
                CASE WHEN sp.TrangThaiXoa=0 AND sp.HanSuDung>GETDATE()
                     AND sp.SoLuongTon>=ct.SoLuong AND ct.SoLuong>0 THEN 1 ELSE 0 END AS Available
            FROM ChiTietGioHang ct WITH (UPDLOCK,HOLDLOCK)
            JOIN SanPham sp WITH (UPDLOCK,ROWLOCK) ON sp.MaSP=ct.MaSP
            OUTER APPLY (
                SELECT TOP 1 c.MaKM,c.GiaKhuyenMai FROM ChiTietKhuyenMai c WITH (UPDLOCK,ROWLOCK)
                JOIN ChuongTrinhKhuyenMai p ON p.MaKM=c.MaKM
                WHERE c.MaSP=sp.MaSP AND p.TrangThai=1 AND GETDATE() BETWEEN p.NgayBatDau AND p.NgayKetThuc
                    AND (c.SoLuongKhuyenMai IS NULL OR ct.SoLuong<=c.SoLuongKhuyenMai-c.SoLuongDaBan)
                ORDER BY c.GiaKhuyenMai,c.MaKM
            ) km
            WHERE ct.MaGioHang=? ORDER BY sp.MaSP
            """,cart);
        if(items.isEmpty())throw new AppException(HttpStatus.BAD_REQUEST,"Giỏ hàng rỗng.");
        BigDecimal total=BigDecimal.ZERO;
        for(var item:items){
            if(((Number)item.get("Available")).intValue()!=1)
                throw new AppException(HttpStatus.CONFLICT,"Sản phẩm đã hết hạn, ngừng bán hoặc không đủ số lượng.");
            total=total.add(((BigDecimal)item.get("DonGia")).multiply(new BigDecimal(item.get("SoLuong").toString())));
        }
        long amount;
        try{amount=total.longValueExact();}catch(ArithmeticException invalid){throw new AppException(HttpStatus.BAD_REQUEST,"SePay yêu cầu số tiền nguyên VND.");}
        if(amount<1000 || amount>50_000_000)throw new AppException(HttpStatus.BAD_REQUEST,"SePay hỗ trợ đơn từ 1.000 đến 50.000.000 đ.");
        var generated=new GeneratedKeyHolder();
        BigDecimal orderTotal=total;
        jdbc.update(connection->{
            var statement=connection.prepareStatement("""
                INSERT INTO DonHang(MaKH,TongTien,DiaChiGiaoHang,GhiChu,TrangThai,NguonDon,PhuongThucThanhToan)
                VALUES(?,?,?,?,'PENDING','SALE','SEPAY')
                """,java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1,customer);statement.setBigDecimal(2,orderTotal);statement.setNString(3,address);statement.setNString(4,note);
            return statement;
        },generated);
        int order=Objects.requireNonNull(generated.getKey()).intValue();
        for(var item:items){
            int quantity=((Number)item.get("SoLuong")).intValue();
            BigDecimal price=(BigDecimal)item.get("DonGia"),original=(BigDecimal)item.get("GiaGoc");
            jdbc.update("""
                INSERT INTO ChiTietDonHang(MaDH,MaSP,SoLuong,DonGia,GiaGoc,TienGiam,MaKM) VALUES(?,?,?,?,?,?,?)
                """,order,item.get("MaSP"),quantity,price,original,original.subtract(price).multiply(BigDecimal.valueOf(quantity)),item.get("MaKM"));
            jdbc.update("UPDATE SanPham SET SoLuongTon=SoLuongTon-?,NgayCapNhat=GETDATE() WHERE MaSP=?",quantity,item.get("MaSP"));
            if(item.get("MaKM")!=null)jdbc.update("UPDATE ChiTietKhuyenMai SET SoLuongDaBan=SoLuongDaBan+? WHERE MaKM=? AND MaSP=?",quantity,item.get("MaKM"),item.get("MaSP"));
        }
        String id="DM"+UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        jdbc.update("""
            INSERT INTO ThanhToanSePay(PaymentId,MaDH,MaKH,IdempotencyKey,AccountNumber,Amount,BankCode,AccountName)
            VALUES(?,?,?,?,?,?,?,?)
            """,id,order,customer,key,account,amount,bank,holder);
        jdbc.update("DELETE FROM ChiTietGioHang WHERE MaGioHang=?",cart);
        jdbc.update("UPDATE GioHang SET NgayCapNhat=GETDATE() WHERE MaGioHang=?",cart);
        return get(id);
    }
    public List<SePayPayment> expired() {
        return jdbc.query("SELECT TOP 20 * FROM ThanhToanSePay WHERE Status='PENDING' AND ExpiresAt<=GETDATE() ORDER BY ExpiresAt", SePayPayment::read);
    }
    public void recordWebhook(String transaction,String payload,String outcome) {
        jdbc.update("INSERT INTO SePayWebhook(TransactionId,Payload,Outcome) VALUES(?,?,?)",transaction,payload,outcome);
    }
    public boolean hasWebhook(String transaction) {
        return !jdbc.queryForList("SELECT TransactionId FROM SePayWebhook WITH (UPDLOCK,HOLDLOCK) WHERE TransactionId=?",transaction).isEmpty();
    }
    private SePayPayment lock(String id) {
        var owner=get(id);
        jdbc.queryForList("SELECT MaKH FROM KhachHang WITH (UPDLOCK,ROWLOCK) WHERE MaKH=?",owner.maKH());
        jdbc.queryForList("SELECT MaDH FROM DonHang WITH (UPDLOCK,ROWLOCK) WHERE MaDH=?",owner.maDH());
        return jdbc.query("SELECT * FROM ThanhToanSePay WITH (UPDLOCK,ROWLOCK) WHERE PaymentId=?",SePayPayment::read,id).getFirst();
    }
    @Transactional
    public void settle(String id,long amount,String transaction) {
        var p=lock(id);
        if(amount!=p.amount())throw new AppException(HttpStatus.BAD_REQUEST,"Số tiền không khớp đơn hàng.");
        if("PAID".equals(p.status())) {
            if(!transaction.equals(p.transId()))throw new AppException(HttpStatus.CONFLICT,"Mã giao dịch không khớp.");
            return;
        }
        // A late transfer cannot buy inventory whose reservation has expired.
        expireLocked(p);
        p=get(id);
        boolean paid=p.pending() && jdbc.update("UPDATE DonHang SET TrangThai='PAID' WHERE MaDH=? AND TrangThai='PENDING'",p.maDH())==1;
        jdbc.update("UPDATE ThanhToanSePay SET Status=?,TransId=?,UpdatedAt=GETDATE() WHERE PaymentId=?",
                paid?"PAID":"REVIEW",transaction,id);
    }
    @Transactional
    public void expire(String id) {expireLocked(lock(id));}
    private void expireLocked(SePayPayment p) {
        if(!p.pending() || jdbc.queryForObject("SELECT COUNT(*) FROM ThanhToanSePay WHERE PaymentId=? AND ExpiresAt<=GETDATE()",Integer.class,p.paymentId())==0)return;
        // Reserve and release stock under the same customer -> order -> product lock order.
        var items=jdbc.queryForList("SELECT MaSP,SoLuong,MaKM FROM ChiTietDonHang WHERE MaDH=? ORDER BY MaSP",p.maDH());
        if(jdbc.update("UPDATE DonHang SET TrangThai='CANCELLED' WHERE MaDH=? AND TrangThai='PENDING'",p.maDH())!=1) {
            jdbc.update("UPDATE ThanhToanSePay SET Status='REVIEW',UpdatedAt=GETDATE() WHERE PaymentId=?",p.paymentId());
            return;
        }
        for(var item:items) {
            jdbc.update("UPDATE SanPham SET SoLuongTon=SoLuongTon+?,NgayCapNhat=GETDATE() WHERE MaSP=?",item.get("SoLuong"),item.get("MaSP"));
            if(item.get("MaKM")!=null)jdbc.update("UPDATE ChiTietKhuyenMai SET SoLuongDaBan=CASE WHEN SoLuongDaBan>=? THEN SoLuongDaBan-? ELSE 0 END WHERE MaKM=? AND MaSP=?",
                    item.get("SoLuong"),item.get("SoLuong"),item.get("MaKM"),item.get("MaSP"));
        }
        jdbc.update("UPDATE ThanhToanSePay SET Status='FAILED',UpdatedAt=GETDATE() WHERE PaymentId=?",p.paymentId());
    }
}
