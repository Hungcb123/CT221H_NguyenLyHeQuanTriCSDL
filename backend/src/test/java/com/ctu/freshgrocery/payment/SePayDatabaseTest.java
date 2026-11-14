package com.ctu.freshgrocery.payment;

import com.ctu.freshgrocery.exception.AppException;
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
@Import({SePayStore.class,SePaySchema.class})
@EnabledIfEnvironmentVariable(named="RUN_DB_INTEGRATION_TESTS",matches="true")
class SePayDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired SePayStore store;
    int customer,product,cart;
    @BeforeEach void fixtures() {
        String key=UUID.randomUUID().toString();
        customer=jdbc.queryForObject("INSERT INTO KhachHang(HoTen,Email,SoDienThoai,SoDuVi) OUTPUT INSERTED.MaKH VALUES('QA SePay',?,?,1000)",Integer.class,key+"@example.invalid",key.replace("-","").substring(0,15));
        int category=jdbc.queryForObject("SELECT TOP 1 MaDanhMuc FROM DanhMuc ORDER BY MaDanhMuc",Integer.class);
        product=jdbc.queryForObject("INSERT INTO SanPham(TenSP,Gia,DonViTinh,SoLuongTon,HanSuDung,MaDanhMuc) OUTPUT INSERTED.MaSP VALUES(?,10000,N'kg',5,DATEADD(DAY,2,GETDATE()),?)",Integer.class,"QA SePay "+key,category);
        cart=jdbc.queryForObject("INSERT INTO GioHang(MaKH) OUTPUT INSERTED.MaGioHang VALUES(?)",Integer.class,customer);
        jdbc.update("INSERT INTO ChiTietGioHang(MaGioHang,MaSP,SoLuong) VALUES(?,?,2)",cart,product);
    }
    SePayPayment reserve() {return store.reserve(customer,"Can Tho","QA","client-request-123456","123456789","MBBank","TEST ACCOUNT");}
    int stock() {return jdbc.queryForObject("SELECT SoLuongTon FROM SanPham WHERE MaSP=?",Integer.class,product);}
    String order(SePayPayment p) {return jdbc.queryForObject("SELECT TrangThai FROM DonHang WHERE MaDH=?",String.class,p.maDH());}
    void overdue(SePayPayment p) {jdbc.update("UPDATE ThanhToanSePay SET ExpiresAt=DATEADD(MINUTE,-1,GETDATE()) WHERE PaymentId=?",p.paymentId());}
    void walletUntouched() {
        assertEquals(0,new BigDecimal("1000").compareTo(jdbc.queryForObject("SELECT SoDuVi FROM KhachHang WHERE MaKH=?",BigDecimal.class,customer)));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM LichSuGiaoDichVi WHERE MaKH=?",Integer.class,customer));
    }
    @Test void reservationIsIdempotentAndUsesServerPrice() {
        var p=reserve();assertEquals(20000,p.amount());assertEquals(3,stock());assertEquals("PENDING",order(p));
        assertEquals(p.paymentId(),reserve().paymentId());
        assertEquals(p.paymentId(),store.reserve(customer,"Other","","different-request-key","123456789","MBBank","TEST").paymentId());
        assertEquals(3,stock());walletUntouched();
    }
    @Test void successOnlyPaysOnceAndNeverDebitsWallet() {
        var p=reserve();store.settle(p.paymentId(),p.amount(),"98765");store.settle(p.paymentId(),p.amount(),"98765");
        assertEquals("PAID",order(p));assertEquals("PAID",store.get(p.paymentId()).status());assertEquals(3,stock());walletUntouched();
        store.expire(p.paymentId());assertEquals(3,stock());
    }
    @Test void expirationOnlyRestoresStockOnce() {
        var p=reserve();store.expire(p.paymentId());assertEquals(3,stock());
        overdue(p);store.expire(p.paymentId());store.expire(p.paymentId());
        assertEquals(5,stock());assertEquals("CANCELLED",order(p));assertEquals("FAILED",store.get(p.paymentId()).status());walletUntouched();
    }
    @Test void lateTransferBeforeSchedulerRunsCannotShipReleasedInventory() {
        var p=reserve();overdue(p);store.settle(p.paymentId(),p.amount(),"98766");
        assertEquals(5,stock());assertEquals("CANCELLED",order(p));assertEquals("REVIEW",store.get(p.paymentId()).status());walletUntouched();
    }
    @Test void lateTransferAfterExpiryNeedsReview() {
        var p=reserve();overdue(p);store.expire(p.paymentId());store.settle(p.paymentId(),p.amount(),"98767");
        assertEquals(5,stock());assertEquals("CANCELLED",order(p));assertEquals("REVIEW",store.get(p.paymentId()).status());walletUntouched();
    }
    @Test void wrongAmountDoesNotConfirmPayment() {
        var p=reserve();assertThrows(AppException.class,()->store.settle(p.paymentId(),1,"98768"));
        assertEquals("PENDING",order(p));assertEquals(3,stock());
    }
    @Test void softDeletedProductCannotBePurchased() {
        jdbc.update("UPDATE SanPham SET TrangThaiXoa=1 WHERE MaSP=?",product);
        assertThrows(AppException.class,this::reserve);assertEquals(5,stock());walletUntouched();
    }
    @Test void softDeletePreservesExistingOrderDetails() {
        var p=reserve();
        jdbc.execute("DECLARE @DaXoa BIT; EXEC dbo.sp_XoaSanPham @MaSP="+product+", @DaXoa=@DaXoa OUTPUT;");
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM SanPham WHERE MaSP=? AND TrangThaiXoa=1",Integer.class,product));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM ChiTietDonHang WHERE MaDH=? AND MaSP=?",Integer.class,p.maDH(),product));
        store.settle(p.paymentId(),p.amount(),"98769");assertEquals("PAID",order(p));
    }
}
