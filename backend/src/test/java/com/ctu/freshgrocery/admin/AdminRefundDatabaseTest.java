package com.ctu.freshgrocery.admin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import(AdminOrderService.class)
@EnabledIfEnvironmentVariable(named="RUN_DB_INTEGRATION_TESTS",matches="true")
class AdminRefundDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AdminOrderService service;
    @Autowired PlatformTransactionManager transactions;
    int customer,order,product,stock;
    @BeforeEach void fixtures(){
        String key=UUID.randomUUID().toString();
        customer=jdbc.queryForObject("INSERT INTO KhachHang (HoTen,Email,SoDienThoai,SoDuVi) OUTPUT INSERTED.MaKH VALUES (N'QA rollback',?,?,1000)",Integer.class,key+"@example.invalid",key.replace("-","").substring(0,15));
        product=jdbc.queryForObject("SELECT TOP 1 MaSP FROM SanPham WHERE TrangThaiXoa=0 ORDER BY MaSP",Integer.class);
        stock=jdbc.queryForObject("SELECT SoLuongTon FROM SanPham WHERE MaSP=?",Integer.class,product);
        order=jdbc.queryForObject("INSERT INTO DonHang (MaKH,TongTien,TrangThai,GhiChu) OUTPUT INSERTED.MaDH VALUES (?,100,'PAID',N'QA rollback only')",Integer.class,customer);
        jdbc.update("INSERT INTO ChiTietDonHang (MaDH,MaSP,SoLuong,DonGia) VALUES (?,?,2,50)",order,product);
    }
    @Test void realRefundIsIdempotentAndBalancesLedger(){
        service.changeStatus(order,"CANCELLED","qa");service.changeStatus(order,"CANCELLED","qa");
        assertEquals(0,new BigDecimal("1100").compareTo(jdbc.queryForObject("SELECT SoDuVi FROM KhachHang WHERE MaKH=?",BigDecimal.class,customer)));
        assertEquals(stock+2,jdbc.queryForObject("SELECT SoLuongTon FROM SanPham WHERE MaSP=?",Integer.class,product));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM LichSuGiaoDichVi WHERE MaDH=? AND LoaiGiaoDich='HOAN_TIEN'",Integer.class,order));
    }
    @Test void failedAuditRollsBackWalletInventoryAndStatus(){
        // Required audit actor exceeds NVARCHAR(100). SQL must reject the final write.
        var tx=new TransactionTemplate(transactions);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
        assertThrows(RuntimeException.class,()->tx.execute(ignored->{service.changeStatus(order,"CANCELLED","x".repeat(1000));return null;}));
        assertEquals("PAID",jdbc.queryForObject("SELECT TrangThai FROM DonHang WHERE MaDH=?",String.class,order));
        assertEquals(stock,jdbc.queryForObject("SELECT SoLuongTon FROM SanPham WHERE MaSP=?",Integer.class,product));
        assertEquals(0,new BigDecimal("1000").compareTo(jdbc.queryForObject("SELECT SoDuVi FROM KhachHang WHERE MaKH=?",BigDecimal.class,customer)));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM LichSuGiaoDichVi WHERE MaDH=?",Integer.class,order));
    }
}
