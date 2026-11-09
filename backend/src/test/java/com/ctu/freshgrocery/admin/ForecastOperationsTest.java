package com.ctu.freshgrocery.admin;
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
        var target=LocalDate.now().plusDays(1);
        when(jdbc.queryForList(anyString(),eq(1),any(java.sql.Date.class),any(java.sql.Date.class))).thenReturn(List.of(Map.of("SaleDate",java.sql.Date.valueOf(target.minusDays(7)),"Quantity",7),Map.of("SaleDate",java.sql.Date.valueOf(target.minusDays(1)),"Quantity",14)));
        var prediction=service.predictForProduct(1);
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
}
