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
public class MoMoStore {
    private final JdbcTemplate jdbc;
    private final MoMoSchema schema;

    public MoMoPayment get(String id) {
        return jdbc.query("SELECT * FROM ThanhToanMoMo WHERE PaymentId=?",MoMoPayment::read,id).stream().findFirst()
                .orElseThrow(()->new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy thanh toán MoMo."));
    }
    public Optional<MoMoPayment> forOrder(int order, int customer) {
        return jdbc.query("SELECT * FROM ThanhToanMoMo WHERE MaDH=? AND MaKH=?",MoMoPayment::read,order,customer).stream().findFirst();
    }
    @Transactional
    public MoMoPayment reserve(int customer, String address, String note, String key, String partner) {
        var customers=jdbc.queryForList("SELECT MaKH FROM KhachHang WITH (UPDLOCK,ROWLOCK) WHERE MaKH=?",customer);
        if(customers.isEmpty())throw new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy khách hàng.");
        var existing=jdbc.query("""
            SELECT TOP 1 * FROM ThanhToanMoMo WITH (UPDLOCK,HOLDLOCK)
            WHERE MaKH=? AND (IdempotencyKey=? OR Status IN ('CREATING','PENDING','REVIEW'))
            ORDER BY CASE WHEN IdempotencyKey=? THEN 0 ELSE 1 END, CreatedAt DESC
            """,MoMoPayment::read,customer,key,key);
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
        try{amount=total.longValueExact();}catch(ArithmeticException invalid){throw new AppException(HttpStatus.BAD_REQUEST,"MoMo yêu cầu số tiền nguyên VND.");}
        if(amount<1000 || amount>50_000_000)throw new AppException(HttpStatus.BAD_REQUEST,"MoMo hỗ trợ đơn từ 1.000 đến 50.000.000 đ.");
        var generated=new GeneratedKeyHolder();
        BigDecimal orderTotal=total;
        jdbc.update(connection->{
            var statement=connection.prepareStatement("""
                INSERT INTO DonHang(MaKH,TongTien,DiaChiGiaoHang,GhiChu,TrangThai,NguonDon,PhuongThucThanhToan)
                VALUES(?,?,?,?,'PENDING','SALE','MOMO_SANDBOX')
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
        String id="DM-"+UUID.randomUUID(), requestId=UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO ThanhToanMoMo(PaymentId,MaDH,MaKH,IdempotencyKey,RequestId,PartnerCode,Amount,OrderInfo)
            VALUES(?,?,?,?,?,?,?,?)
            """,id,order,customer,key,requestId,partner,amount,"DailyMart sandbox - Don #"+order);
        jdbc.update("DELETE FROM ChiTietGioHang WHERE MaGioHang=?",cart);
        jdbc.update("UPDATE GioHang SET NgayCapNhat=GETDATE() WHERE MaGioHang=?",cart);
        return get(id);
    }
    public void setPayUrl(String id,String url){
        jdbc.update("""
            UPDATE ThanhToanMoMo SET PayUrl=?,Status=CASE WHEN Status='CREATING' THEN 'PENDING' ELSE Status END,
                UpdatedAt=GETDATE() WHERE PaymentId=?
            """,url,id);
    }
    public void note(String id,String message){
        jdbc.update("UPDATE ThanhToanMoMo SET Message=?,UpdatedAt=GETDATE() WHERE PaymentId=?",shortMessage(message),id);
    }
    public boolean claimQuery(String id){
        return jdbc.update("""
            UPDATE ThanhToanMoMo SET LastCheckedAt=GETDATE()
            WHERE PaymentId=? AND Status IN ('CREATING','PENDING')
                AND (LastCheckedAt IS NULL OR LastCheckedAt<DATEADD(SECOND,-30,GETDATE()))
            """,id)==1;
    }
    public List<MoMoPayment> awaitingReconciliation(){
        return jdbc.query("""
            SELECT TOP 5 * FROM ThanhToanMoMo WHERE Status IN ('CREATING','PENDING')
                AND CreatedAt<DATEADD(SECOND,-60,GETDATE())
                AND (LastCheckedAt IS NULL OR LastCheckedAt<DATEADD(SECOND,-60,GETDATE())) ORDER BY CreatedAt
            """,MoMoPayment::read);
    }
    @Transactional
    public void apply(String id, long amount, int code, String transaction, String message) {
        var owner=get(id);
        jdbc.queryForList("SELECT MaKH FROM KhachHang WITH (UPDLOCK,ROWLOCK) WHERE MaKH=?",owner.maKH());
        jdbc.queryForList("SELECT MaDH FROM DonHang WITH (UPDLOCK,ROWLOCK) WHERE MaDH=?",owner.maDH());
        var payment=jdbc.query("SELECT * FROM ThanhToanMoMo WITH (UPDLOCK,ROWLOCK) WHERE PaymentId=?",MoMoPayment::read,id).getFirst();
        if(amount!=payment.amount())throw new AppException(HttpStatus.BAD_REQUEST,"Số tiền MoMo không khớp đơn hàng.");
        if(code==0){
            if(transaction==null || !transaction.matches("[1-9][0-9]*"))throw new AppException(HttpStatus.BAD_REQUEST,"Thiếu mã giao dịch MoMo.");
            if("PAID".equals(payment.status())){
                if(!transaction.equals(payment.transId()))throw new AppException(HttpStatus.CONFLICT,"Mã giao dịch không khớp.");
                return;
            }
            if(!payment.pending()){
                jdbc.update("UPDATE ThanhToanMoMo SET Status='REVIEW',TransId=?,ResultCode=?,Message=?,UpdatedAt=GETDATE() WHERE PaymentId=?",
                        transaction,code,"MoMo báo đã nhận tiền sau trạng thái kết thúc; cần đối soát.",id);
                return;
            }
            if(jdbc.update("UPDATE DonHang SET TrangThai='PAID' WHERE MaDH=? AND TrangThai='PENDING'",payment.maDH())!=1){
                jdbc.update("UPDATE ThanhToanMoMo SET Status='REVIEW',TransId=?,ResultCode=0,Message=?,UpdatedAt=GETDATE() WHERE PaymentId=?",
                        transaction,"Trạng thái đơn không còn chờ thanh toán; cần đối soát.",id);
                return;
            }
            jdbc.update("UPDATE ThanhToanMoMo SET Status='PAID',TransId=?,ResultCode=0,Message=?,UpdatedAt=GETDATE() WHERE PaymentId=?",transaction,shortMessage(message),id);
        }else if(terminalFailure(code) && payment.pending()){
            var items=jdbc.queryForList("SELECT MaSP,SoLuong,MaKM FROM ChiTietDonHang WHERE MaDH=? ORDER BY MaSP",payment.maDH());
            for(var item:items){
                jdbc.update("UPDATE SanPham SET SoLuongTon=SoLuongTon+?,NgayCapNhat=GETDATE() WHERE MaSP=?",item.get("SoLuong"),item.get("MaSP"));
                if(item.get("MaKM")!=null)jdbc.update("UPDATE ChiTietKhuyenMai SET SoLuongDaBan=CASE WHEN SoLuongDaBan>=? THEN SoLuongDaBan-? ELSE 0 END WHERE MaKM=? AND MaSP=?",
                        item.get("SoLuong"),item.get("SoLuong"),item.get("MaKM"),item.get("MaSP"));
            }
            var carts=jdbc.queryForList("SELECT MaGioHang FROM GioHang WITH (UPDLOCK,HOLDLOCK) WHERE MaKH=?",payment.maKH());
            int cart;
            if(carts.isEmpty()){
                cart=Objects.requireNonNull(jdbc.queryForObject("INSERT INTO GioHang(MaKH,NgayCapNhat) OUTPUT INSERTED.MaGioHang VALUES(?,GETDATE())",Integer.class,payment.maKH()));
            }else cart=((Number)carts.getFirst().get("MaGioHang")).intValue();
            for(var item:items){
                if(jdbc.update("UPDATE ChiTietGioHang SET SoLuong=SoLuong+? WHERE MaGioHang=? AND MaSP=?",item.get("SoLuong"),cart,item.get("MaSP"))==0)
                    jdbc.update("INSERT INTO ChiTietGioHang(MaGioHang,MaSP,SoLuong) VALUES(?,?,?)",cart,item.get("MaSP"),item.get("SoLuong"));
            }
            jdbc.update("UPDATE DonHang SET TrangThai='CANCELLED' WHERE MaDH=? AND TrangThai='PENDING'",payment.maDH());
            jdbc.update("UPDATE ThanhToanMoMo SET Status='FAILED',ResultCode=?,Message=?,UpdatedAt=GETDATE() WHERE PaymentId=?",code,shortMessage(message),id);
        }else if(payment.pending()){
            jdbc.update("UPDATE ThanhToanMoMo SET ResultCode=?,Message=?,UpdatedAt=GETDATE() WHERE PaymentId=?",code,shortMessage(message),id);
        }
        // No internal-wallet credits/debits: MoMo payments have their own audit record.
    }
    public static boolean terminalFailure(int code){return Set.of(98,1001,1002,1003,1004,1005,1006,1007,1017,1026,4001,4002,4100).contains(code);}
    private String shortMessage(String message){return message==null?"":message.substring(0,Math.min(message.length(),500));}
}
