package com.ctu.freshgrocery.admin;
import com.ctu.freshgrocery.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class AdminOrderServiceTest {
    JdbcTemplate jdbc=mock(JdbcTemplate.class);
    AdminOrderService service=new AdminOrderService(jdbc);
    void order(String state){
        when(jdbc.queryForList("SELECT MaKH FROM DonHang WHERE MaDH = ? AND NguonDon = 'SALE'",1)).thenReturn(List.of(Map.of("MaKH",2)));
        when(jdbc.queryForObject(anyString(),eq(BigDecimal.class),eq(2))).thenReturn(new BigDecimal("500"));
        when(jdbc.queryForMap(anyString(),eq(1))).thenReturn(Map.of("TrangThai",state,"TongTien",new BigDecimal("100")));
    }
    @Test void disallowsManufacturingPaidOrders(){assertThrows(AppException.class,()->service.changeStatus(1,"PAID","admin"));verifyNoInteractions(jdbc);}
    @Test void repeatedCancellationDoesNotRefundTwice(){order("CANCELLED");assertEquals(false,service.changeStatus(1,"CANCELLED","admin").get("refunded"));verify(jdbc,never()).update(anyString(),any(Object[].class));}
    @Test void deliveredOrderCannotBeCancelled(){order("DELIVERED");assertThrows(AppException.class,()->service.changeStatus(1,"CANCELLED","admin"));verify(jdbc,never()).update(anyString(),any(Object[].class));}
    @Test void refusesRefundOfBenchmarkOrders(){order("PAID");when(jdbc.queryForList(contains("ChiTietDonHang"),eq(1))).thenReturn(List.of());assertThrows(AppException.class,()->service.changeStatus(1,"CANCELLED","admin"));verify(jdbc,never()).update(anyString(),any(Object[].class));}
    @Test void paidCancellationRestoresStockQuotaAndWallet(){order("PAID");when(jdbc.queryForList(contains("ChiTietDonHang"),eq(1))).thenReturn(List.of(Map.of("MaSP",3,"SoLuong",2,"MaKM",4)));assertEquals(true,service.changeStatus(1,"CANCELLED","admin").get("refunded"));
        verify(jdbc).update("UPDATE KhachHang SET SoDuVi = SoDuVi + ? WHERE MaKH = ?",new BigDecimal("100"),2);
        verify(jdbc).update("UPDATE SanPham SET SoLuongTon = SoLuongTon + ?, NgayCapNhat = GETDATE() WHERE MaSP = ?",2,3);
        verify(jdbc).update(startsWith("UPDATE ChiTietKhuyenMai"),eq(2),eq(2),eq(4),eq(3));
        verify(jdbc).update(startsWith("INSERT INTO LichSuGiaoDichVi"),eq(2),eq(1),eq(new BigDecimal("500")),eq(new BigDecimal("100")),eq(new BigDecimal("600")),anyString());
        verify(jdbc).update("UPDATE DonHang SET TrangThai = ? WHERE MaDH = ?","CANCELLED",1);
    }
    @Test void pendingCancellationNeverRefunds(){order("PENDING");assertEquals(false,service.changeStatus(1,"CANCELLED","admin").get("refunded"));verify(jdbc,never()).update(startsWith("UPDATE KhachHang"),any(),any());}
    @Test void momoCannotBeRefundedIntoInternalWallet(){
        order("PAID");when(jdbc.queryForMap(anyString(),eq(1))).thenReturn(Map.of("TrangThai","PAID","TongTien",new BigDecimal("100"),"PhuongThucThanhToan","MOMO_SANDBOX"));
        assertThrows(AppException.class,()->service.changeStatus(1,"CANCELLED","admin"));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
    @Test void sepayCannotBeRefundedIntoInternalWallet(){
        order("PAID");when(jdbc.queryForMap(anyString(),eq(1))).thenReturn(Map.of("TrangThai","PAID","TongTien",new BigDecimal("100"),"PhuongThucThanhToan","SEPAY"));
        assertThrows(AppException.class,()->service.changeStatus(1,"CANCELLED","admin"));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
}
