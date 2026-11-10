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
@Import({MoMoStore.class,MoMoSchema.class})
@EnabledIfEnvironmentVariable(named="RUN_DB_INTEGRATION_TESTS",matches="true")
class MoMoDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MoMoStore store;
    int customer,product,cart;
    @BeforeEach void fixtures(){
        String key=UUID.randomUUID().toString();
        customer=jdbc.queryForObject("INSERT INTO KhachHang(HoTen,Email,SoDienThoai,SoDuVi) OUTPUT INSERTED.MaKH VALUES('QA MoMo',?,?,1000)",Integer.class,key+"@example.invalid",key.replace("-","").substring(0,15));
        int category=jdbc.queryForObject("SELECT TOP 1 MaDanhMuc FROM DanhMuc ORDER BY MaDanhMuc",Integer.class);
        product=jdbc.queryForObject("""
            INSERT INTO SanPham(TenSP,Gia,DonViTinh,SoLuongTon,HanSuDung,MaDanhMuc)
            OUTPUT INSERTED.MaSP VALUES(?,10000,N'kg',5,DATEADD(DAY,2,GETDATE()),?)
            """,Integer.class,"QA MoMo "+key,category);
        cart=jdbc.queryForObject("INSERT INTO GioHang(MaKH) OUTPUT INSERTED.MaGioHang VALUES(?)",Integer.class,customer);
        jdbc.update("INSERT INTO ChiTietGioHang(MaGioHang,MaSP,SoLuong) VALUES(?,?,2)",cart,product);
    }
    MoMoPayment reserve(){return store.reserve(customer,"Can Tho","QA","client-request-123456","partner");}
    int stock(){return jdbc.queryForObject("SELECT SoLuongTon FROM SanPham WHERE MaSP=?",Integer.class,product);}
    String orderState(MoMoPayment p){return jdbc.queryForObject("SELECT TrangThai FROM DonHang WHERE MaDH=?",String.class,p.maDH());}
    void walletUntouched(){
        assertEquals(0,new BigDecimal("1000").compareTo(jdbc.queryForObject("SELECT SoDuVi FROM KhachHang WHERE MaKH=?",BigDecimal.class,customer)));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM LichSuGiaoDichVi WHERE MaKH=?",Integer.class,customer));
    }
    @Test void reservationAndRetryDoNotDeductTwiceOrPayEarly(){
        var payment=reserve();assertEquals(3,stock());assertEquals("PENDING",orderState(payment));assertEquals("CREATING",payment.status());
        assertEquals(20000,payment.amount());assertEquals(payment.paymentId(),reserve().paymentId());
        assertEquals(payment.paymentId(),store.reserve(customer,"Other address","","different-request-key","partner").paymentId());
        assertEquals(3,stock());walletUntouched();
        store.setPayUrl(payment.paymentId(),"https://test-payment.momo.vn/pay");
        assertEquals("PENDING",orderState(payment));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ChiTietGioHang WHERE MaGioHang=?",Integer.class,cart));
    }
    @Test void duplicateSuccessChangesStatusOnceWithoutDebitingWallet(){
        var payment=reserve();store.apply(payment.paymentId(),payment.amount(),0,"987654321","success");
        store.apply(payment.paymentId(),payment.amount(),0,"987654321","success");
        assertEquals("PAID",orderState(payment));assertEquals("PAID",store.get(payment.paymentId()).status());assertEquals(3,stock());walletUntouched();
        store.apply(payment.paymentId(),payment.amount(),1006,"0","late failure");
        assertEquals("PAID",orderState(payment));assertEquals(3,stock());
    }
    @Test void failureRestoresStockAndCartOnlyOnce(){
        var payment=reserve();store.apply(payment.paymentId(),payment.amount(),1006,"0","cancelled");
        store.apply(payment.paymentId(),payment.amount(),1006,"0","cancelled");
        assertEquals(5,stock());assertEquals("CANCELLED",orderState(payment));assertEquals("FAILED",store.get(payment.paymentId()).status());
        assertEquals(2,jdbc.queryForObject("SELECT SoLuong FROM ChiTietGioHang WHERE MaGioHang=? AND MaSP=?",Integer.class,cart,product));walletUntouched();
    }
    @Test void lateSuccessAfterFailureRequiresReviewInsteadOfShippingOrDeductingAgain(){
        var payment=reserve();store.apply(payment.paymentId(),payment.amount(),1005,"0","expired");
        store.apply(payment.paymentId(),payment.amount(),0,"987654322","late success");
        assertEquals("REVIEW",store.get(payment.paymentId()).status());assertEquals("CANCELLED",orderState(payment));assertEquals(5,stock());walletUntouched();
    }
    @Test void wrongAmountCannotMarkOrderPaid(){
        var payment=reserve();assertThrows(AppException.class,()->store.apply(payment.paymentId(),1,0,"987654323","wrong"));
        assertEquals("PENDING",orderState(payment));walletUntouched();
    }
    @Test void unknownAndAuthorizedResultsKeepReservation(){
        var payment=reserve();store.apply(payment.paymentId(),payment.amount(),9000,"987654324","authorized");
        assertEquals("PENDING",orderState(payment));assertTrue(store.get(payment.paymentId()).pending());assertEquals(3,stock());
        store.apply(payment.paymentId(),payment.amount(),7000,"0","processing");assertEquals(3,stock());walletUntouched();
    }
    @Test void expiredProductsRejectReservationWithoutChangingAnything(){
        jdbc.update("UPDATE SanPham SET HanSuDung=DATEADD(DAY,-1,GETDATE()) WHERE MaSP=?",product);
        assertThrows(AppException.class,this::reserve);assertEquals(5,stock());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM DonHang WHERE MaKH=?",Integer.class,customer));walletUntouched();
    }
}
