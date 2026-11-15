package com.ctu.freshgrocery.admin;
import com.ctu.freshgrocery.dto.ForecastCustomRequest;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.repository.*;
import com.ctu.freshgrocery.service.impl.DemandForecastServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class ForecastOperationsTest {
    ProductRepository products=mock(ProductRepository.class);
    DemandForecastRepository history=mock(DemandForecastRepository.class);
    ForecastSpRepository sp=mock(ForecastSpRepository.class);
    JdbcTemplate jdbc=mock(JdbcTemplate.class);
    DemandForecastServiceImpl service=new DemandForecastServiceImpl(products,history,sp,jdbc);
    @BeforeEach void setup(){
        when(products.findById(1)).thenReturn(Optional.of(Product.builder().maSP(1).tenSP("Test").donViTinh("kg").gia(new BigDecimal("75000")).soLuongTon(20).hanSuDung(LocalDateTime.now().plusDays(5)).trangThaiXoa(false).build()));
        when(sp.executeLuuKetQuaDuBao(eq(1),any(),any(),any(),any(),isNull(),anyString(),anyString())).thenReturn(Map.of("StatusCode",1,"MaDuBao_Output",12L,"Message","ok"));
    }
    @AfterEach void close(){service.cleanup();}
    @Test void uninitializedModelFailsInsteadOfGeneratingFakePrediction(){
        assertEquals(false,service.getModelStatus().get("ready"));
        assertThrows(AppException.class,()->service.predictForProduct(1));
        verifyNoInteractions(sp);
    }
    @Test void usesActualSalesAndZeroFillsMissingDates(){
        service.init(); assertEquals(true,service.getModelStatus().get("ready"));
        var target=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        when(jdbc.queryForList(anyString(),eq(1),any(java.sql.Date.class),any(java.sql.Date.class))).thenReturn(List.of(Map.of("SaleDate",java.sql.Date.valueOf(target.minusDays(7)),"Quantity",7),Map.of("SaleDate",java.sql.Date.valueOf(target.minusDays(1)),"Quantity",14)));
        var prediction=service.predictForProduct(1);
        assertEquals(target,prediction.getNgayDuBao());
        assertEquals(14.0,prediction.getFeaturesUsed().get("Lag_1"));
        assertEquals(7.0,prediction.getFeaturesUsed().get("Lag_7"));
        assertEquals(3.0,prediction.getFeaturesUsed().get("Rolling_Mean_7"));
        assertNull(prediction.getDoTinCay());assertEquals(12L,prediction.getMaDuBao());
    }
    @Test void noSalesStaysZeroAndPersistenceFailureIsNotSuccess(){
        service.init();
        when(sp.executeLuuKetQuaDuBao(eq(1),any(),any(),any(),any(),isNull(),anyString(),anyString())).thenReturn(Map.of("StatusCode",-99,"Message","write failed"));
        assertThrows(AppException.class,()->service.predictForProduct(1));
    }
    @Test void databaseErrorsDoNotBecomeSyntheticHistory(){
        service.init();
        when(jdbc.queryForList(anyString(),eq(1),any(java.sql.Date.class),any(java.sql.Date.class))).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));
        assertThrows(org.springframework.dao.DataAccessException.class,()->service.predictForProduct(1));verifyNoInteractions(sp);
    }
    @Test void highRiskSimulationNeverSavesForecastOrAlert() {
        service.init();
        var request = ForecastCustomRequest.builder()
                .maSP(1).targetDate(LocalDate.of(2026, 10, 17))
                .currentStock(150).daysUntilExpiry(1)
                .lag1(48.0).lag7(45.0).rollingMean7(46.5).rollingStd7(4.2)
                .build();
        var result = service.predictCustom(request);
        assertEquals("CAO", result.getRuiRoHuHong());
        assertNull(result.getMaDuBao());
        assertNull(result.getCanhBaoLienKet());
        assertEquals("PyTorch-MLP-ONNX", result.getThuatToan());
        verifyNoInteractions(sp);
    }

    @Test void newProductWithOneSaleUsesRealHistoryRatherThanNumericIdentifier() {
        service.init();
        var target = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        for (int id : new int[]{1204, 90004}) {
            when(products.findById(id)).thenReturn(Optional.of(Product.builder()
                    .maSP(id).tenSP("New product").donViTinh("hộp").gia(new BigDecimal("50000"))
                    .soLuongTon(9).hanSuDung(LocalDateTime.now().plusDays(15)).build()));
            when(jdbc.queryForList(anyString(), eq(id), any(java.sql.Date.class), any(java.sql.Date.class)))
                    .thenReturn(List.of(Map.of("SaleDate", java.sql.Date.valueOf(target.minusDays(1)), "Quantity", 1)));
            when(sp.executeLuuKetQuaDuBao(eq(id), any(), any(), any(), any(), isNull(), anyString(), anyString()))
                    .thenReturn(Map.of("StatusCode", 1, "MaDuBao_Output", 20L));
            var result = service.predictForProduct(id);
            assertEquals(0.14, result.getSoLuongDuBaoTieuThu());
            assertEquals(0, result.getSoLuongDeXuatNhap());
            assertEquals("SMA7_NEW_PRODUCT", result.getThuatToan());
            assertNotNull(result.getGhiChuDuBao());
            verify(sp).executeLuuKetQuaDuBao(eq(id), eq(target), eq(new BigDecimal("0.14")),
                    eq(9), eq(0), isNull(), eq("SMA7_NEW_PRODUCT"), eq("TRUNG_BINH"));
        }
    }

    @Test void knownProductWithSparseOrZeroSalesDoesNotInheritSyntheticDemand() {
        service.init();
        var target = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        when(jdbc.queryForList(anyString(), eq(1), any(java.sql.Date.class), any(java.sql.Date.class)))
                .thenReturn(List.of(Map.of("SaleDate", java.sql.Date.valueOf(target.minusDays(1)), "Quantity", 1)))
                .thenReturn(List.of());
        var sparse = service.predictForProduct(1);
        assertEquals("SMA7_SPARSE_HISTORY", sparse.getThuatToan());
        assertEquals(0.14, sparse.getSoLuongDuBaoTieuThu());
        var zero = service.predictForProduct(1);
        assertEquals(0.0, zero.getSoLuongDuBaoTieuThu());
        assertEquals(0, zero.getSoLuongDeXuatNhap());
        assertEquals("SMA7_SPARSE_HISTORY", zero.getThuatToan());
    }

    @Test void unknownProductSimulationUsesMeanAndNeverWrites() {
        service.init();
        when(products.findById(1204)).thenReturn(Optional.of(Product.builder().maSP(1204).tenSP("New")
                .gia(new BigDecimal("50000")).soLuongTon(9).hanSuDung(LocalDateTime.now().plusDays(10)).build()));
        var result = service.predictCustom(ForecastCustomRequest.builder().maSP(1204)
                .lag1(30.0).lag7(32.0).rollingMean7(31.0).rollingStd7(3.5)
                .currentStock(80).daysUntilExpiry(1).build());
        assertEquals(31.0, result.getSoLuongDuBaoTieuThu());
        assertEquals(0, result.getSoLuongDeXuatNhap());
        assertEquals("CAO", result.getRuiRoHuHong());
        assertEquals("SMA7_NEW_PRODUCT", result.getThuatToan());
        assertNull(result.getMaDuBao());
        verifyNoInteractions(sp);
    }

    @Test void unusualPriceAndIntermittentSalesStayOutsideOnnxDomain() {
        service.init();
        var unusual = service.predictCustom(ForecastCustomRequest.builder().maSP(1)
                .unitPrice(new BigDecimal("9000000")).lag1(48.0).lag7(45.0)
                .rollingMean7(46.5).rollingStd7(4.2).build());
        assertEquals("SMA7_OUT_OF_RANGE", unusual.getThuatToan());
        assertEquals(46.5, unusual.getSoLuongDuBaoTieuThu());
        var intermittent = service.predictCustom(ForecastCustomRequest.builder().maSP(1)
                .lag1(70.0).lag7(0.0).rollingMean7(10.0).rollingStd7(26.46).build());
        assertEquals("SMA7_INTERMITTENT_HISTORY", intermittent.getThuatToan());
        assertEquals(10.0, intermittent.getSoLuongDuBaoTieuThu());
        verifyNoInteractions(sp);
    }

    @Test void historyKeepsTheActualForecastMethodAndReasonAfterReload() {
        when(products.existsById(1)).thenReturn(true);
        when(history.findByMaSPOrderByNgayTaoDesc(1)).thenReturn(List.of(
                com.ctu.freshgrocery.model.DemandForecast.builder().maDuBao(20L).maSP(1)
                        .ngayDuBao(LocalDate.of(2026,11,16)).soLuongDuBaoTieuThu(new BigDecimal("0.14"))
                        .soLuongTonHienTai(9).soLuongDeXuatNhap(0).thuatToan("SMA7_SPARSE_HISTORY")
                        .ruiRoHuHong("TRUNG_BINH").build()));
        var saved = service.getForecastHistory(1).getFirst();
        assertEquals("SMA7_SPARSE_HISTORY", saved.getThuatToan());
        assertTrue(saved.getGhiChuDuBao().contains("Lịch sử bán ít"));
        assertEquals(0.14, saved.getSoLuongDuBaoTieuThu());
        verifyNoInteractions(sp);
    }
}
