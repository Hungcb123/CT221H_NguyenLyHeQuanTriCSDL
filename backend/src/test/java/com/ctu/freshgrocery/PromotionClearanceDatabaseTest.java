package com.ctu.freshgrocery;

import com.ctu.freshgrocery.repository.PromotionSpRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PromotionSpRepository.class)
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class PromotionClearanceDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PromotionSpRepository promotions;
    @BeforeEach void procedure() throws Exception {
        jdbc.execute(new ClassPathResource("db/promotion-clearance.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    int product(String name,Integer parent,int stock,int hours,boolean hidden) {
        return jdbc.queryForObject("INSERT INTO SanPham (TenSP, Gia, DonViTinh, SoLuongTon, HanSuDung, MaDanhMuc, MaSPCha, TenLoai, TrangThaiXoa) OUTPUT INSERTED.MaSP VALUES (?,500,N'gói',?,DATEADD(hour,?,GETDATE()),(SELECT TOP 1 MaDanhMuc FROM DanhMuc),?,?,?)",Integer.class,name,stock,hours,parent,parent==null?null:name,hidden);
    }
    @Test void scannerUsesOnlyPurchasableUnexpiredSkusAndDoesNotRoundCheapPricesToZero() {
        int parent=product("[TEST] Clearance parent",null,8,1,false);
        int child=product("[TEST] Clearance type",parent,8,1,false);
        int expired=product("[TEST] Expired clearance",null,8,-1,false);
        int empty=product("[TEST] Empty clearance",null,0,1,false);
        int hidden=product("[TEST] Hidden clearance",null,8,1,true);
        int blocked=product("[TEST] Blocked clearance",hidden,8,1,false);
        var result=promotions.executeTuDongKichHoatXaKhoCanDate(2,new BigDecimal("30"));
        assertEquals(1,result.get("StatusCode"));
        int campaign=(Integer)result.get("MaKM_Output");
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM ChiTietKhuyenMai WHERE MaKM=? AND MaSP IN (?,?,?,?,?)",Integer.class,campaign,parent,expired,empty,hidden,blocked));
        assertEquals(0,jdbc.queryForObject("SELECT GiaKhuyenMai FROM ChiTietKhuyenMai WHERE MaKM=? AND MaSP=?",BigDecimal.class,campaign,child).compareTo(new BigDecimal("350")));
    }
    @Test void rejectsInvalidDiscountRatherThanReportingSuccess() {
        assertEquals(-1,promotions.executeTuDongKichHoatXaKhoCanDate(2,new BigDecimal("100")).get("StatusCode"));
        assertEquals(-1,promotions.executeTuDongKichHoatXaKhoCanDate(0,new BigDecimal("30")).get("StatusCode"));
    }
}
