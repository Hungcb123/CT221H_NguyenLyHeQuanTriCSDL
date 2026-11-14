package com.ctu.freshgrocery.payment;

import com.ctu.freshgrocery.admin.AdminOrderService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({SePaySchema.class, CodSchema.class, AdminOrderService.class})
@EnabledIfEnvironmentVariable(named="RUN_DB_INTEGRATION_TESTS",matches="true")
class CodDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AdminOrderService admin;
    int customer,product,cart;
    @BeforeEach void fixtures() {
        String key=UUID.randomUUID().toString();
        customer=jdbc.queryForObject("INSERT INTO KhachHang(HoTen,Email,SoDienThoai,SoDuVi) OUTPUT INSERTED.MaKH VALUES('QA COD',?,?,0)",Integer.class,key+"@example.invalid",key.replace("-","").substring(0,15));
        int category=jdbc.queryForObject("SELECT TOP 1 MaDanhMuc FROM DanhMuc ORDER BY MaDanhMuc",Integer.class);
        product=jdbc.queryForObject("INSERT INTO SanPham(TenSP,Gia,DonViTinh,SoLuongTon,HanSuDung,MaDanhMuc) OUTPUT INSERTED.MaSP VALUES(?,500,N'goi',5,DATEADD(DAY,2,GETDATE()),?)",Integer.class,"QA COD "+key,category);
        cart=jdbc.queryForObject("INSERT INTO GioHang(MaKH) OUTPUT INSERTED.MaGioHang VALUES(?)",Integer.class,customer);
        jdbc.update("INSERT INTO ChiTietGioHang(MaGioHang,MaSP,SoLuong) VALUES(?,?,2)",cart,product);
    }
    int place() {
        var row=jdbc.queryForMap("""
            DECLARE @id INT,@status INT,@message NVARCHAR(255);
            EXEC sp_ThanhToanDonHang @MaKH=?,@MaGioHang=?,@DiaChiGiaoHang=N'Can Tho',
                @MaDH_Output=@id OUTPUT,@StatusCode=@status OUTPUT,@Message=@message OUTPUT,@PhuongThucThanhToan='COD';
            SELECT @id AS id,@status AS status,@message AS message;
            """,customer,cart);
        assertEquals(1,row.get("status"),String.valueOf(row.get("message")));
        return ((Number)row.get("id")).intValue();
    }
    int checkoutStatus() {
        return jdbc.queryForObject("""
            DECLARE @id INT,@status INT,@message NVARCHAR(255);
            EXEC sp_ThanhToanDonHang @MaKH=?,@MaGioHang=?,@DiaChiGiaoHang=N'Can Tho',
                @MaDH_Output=@id OUTPUT,@StatusCode=@status OUTPUT,@Message=@message OUTPUT,@PhuongThucThanhToan='COD';
            SELECT @status;
            """,Integer.class,customer,cart);
    }
    int addType(int parent, boolean hidden) {
        int category=jdbc.queryForObject("SELECT MaDanhMuc FROM SanPham WHERE MaSP=?",Integer.class,parent);
        return jdbc.queryForObject("INSERT INTO SanPham(MaSPCha,TenLoai,TenSP,Gia,DonViTinh,SoLuongTon,HanSuDung,MaDanhMuc,TrangThaiXoa) OUTPUT INSERTED.MaSP VALUES(?,N'500g',N'QA child',500,N'goi',5,DATEADD(DAY,2,GETDATE()),?,?)",Integer.class,parent,category,hidden);
    }
    @Test void aBaseRowCannotCheckoutWhenAllItsTypesAreHidden() {
        addType(product,true);
        assertEquals(-3,checkoutStatus());
    }
    @Test void existingCartCannotCheckoutATypeAfterItsFamilyIsHidden() {
        int child=addType(product,false);
        jdbc.update("UPDATE ChiTietGioHang SET MaSP=? WHERE MaGioHang=? AND MaSP=?",child,cart,product);
        jdbc.update("UPDATE SanPham SET TrangThaiXoa=1 WHERE MaSP=?",product);
        assertEquals(-3,checkoutStatus());
    }
    int stock(){return jdbc.queryForObject("SELECT SoLuongTon FROM SanPham WHERE MaSP=?",Integer.class,product);}
    void walletUntouched(){
        assertEquals(0,BigDecimal.ZERO.compareTo(jdbc.queryForObject("SELECT SoDuVi FROM KhachHang WHERE MaKH=?",BigDecimal.class,customer)));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM LichSuGiaoDichVi WHERE MaKH=?",Integer.class,customer));
    }
    @Test void zeroWalletCanPlaceCodAndReservesStock(){
        int id=place();assertEquals(3,stock());walletUntouched();
        assertEquals("PENDING",jdbc.queryForObject("SELECT TrangThai FROM DonHang WHERE MaDH=?",String.class,id));
        assertEquals("COD",jdbc.queryForObject("SELECT PhuongThucThanhToan FROM DonHang WHERE MaDH=?",String.class,id));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ChiTietGioHang WHERE MaGioHang=?",Integer.class,cart));
    }
    @Test void cancellationReturnsStockOnceWithoutWalletRefund(){
        int id=place();admin.changeStatus(id,"CANCELLED","QA");assertEquals(5,stock());walletUntouched();
        admin.changeStatus(id,"CANCELLED","QA");assertEquals(5,stock());walletUntouched();
    }
    @Test void deliveryCollectsCodWithoutTouchingWallet(){
        int id=place();admin.changeStatus(id,"DELIVERED","QA");
        assertEquals("DELIVERED",jdbc.queryForObject("SELECT TrangThai FROM DonHang WHERE MaDH=?",String.class,id));
        assertEquals(3,stock());walletUntouched();
    }

    @Test void cancellationRestoresPromotionQuotaExactlyOnce() {
        int promotion=jdbc.queryForObject("""
            INSERT INTO ChuongTrinhKhuyenMai(TenKM,LoaiKM,NgayBatDau,NgayKetThuc)
            OUTPUT INSERTED.MaKM VALUES(N'QA COD quota','FLASH_SALE',DATEADD(DAY,-1,GETDATE()),DATEADD(DAY,1,GETDATE()))
            """,Integer.class);
        jdbc.update("INSERT INTO ChiTietKhuyenMai(MaKM,MaSP,GiaKhuyenMai,SoLuongKhuyenMai,SoLuongDaBan) VALUES(?,?,400,10,0)",promotion,product);
        int id=place();
        assertEquals(2,jdbc.queryForObject("SELECT SoLuongDaBan FROM ChiTietKhuyenMai WHERE MaKM=? AND MaSP=?",Integer.class,promotion,product));
        admin.changeStatus(id,"CANCELLED","QA");admin.changeStatus(id,"CANCELLED","QA");
        assertEquals(0,jdbc.queryForObject("SELECT SoLuongDaBan FROM ChiTietKhuyenMai WHERE MaKM=? AND MaSP=?",Integer.class,promotion,product));
        assertEquals(5,stock());walletUntouched();
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM NhatKyCanhBao WHERE LoaiSuKien='ADMIN_ORDER_STATUS' AND NoiDung LIKE ?",Integer.class,"Đơn #"+id+":%"));
    }
    @Test void unpaidExternalPaymentCannotBeDeliveredOrRefundedByAdmin() {
        int id=place();
        jdbc.update("UPDATE DonHang SET PhuongThucThanhToan='SEPAY' WHERE MaDH=?",id);
        assertThrows(com.ctu.freshgrocery.exception.AppException.class,()->admin.changeStatus(id,"DELIVERED","QA"));
        assertThrows(com.ctu.freshgrocery.exception.AppException.class,()->admin.changeStatus(id,"CANCELLED","QA"));
        assertEquals("PENDING",jdbc.queryForObject("SELECT TrangThai FROM DonHang WHERE MaDH=?",String.class,id));
        assertEquals(3,stock());walletUntouched();
    }
}
