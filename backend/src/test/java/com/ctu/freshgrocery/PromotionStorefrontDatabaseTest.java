package com.ctu.freshgrocery;

import com.ctu.freshgrocery.service.PromotionStorefrontService;
import com.ctu.freshgrocery.service.ProductTypeService;
import com.ctu.freshgrocery.repository.ProductSpRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PromotionStorefrontService.class, ProductSpRepository.class, ProductTypeService.class})
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class PromotionStorefrontDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PromotionStorefrontService service;
    int campaign;
    @BeforeEach void setup() {
        campaign = jdbc.queryForObject("INSERT INTO ChuongTrinhKhuyenMai (TenKM, LoaiKM, NgayBatDau, NgayKetThuc, TrangThai) OUTPUT INSERTED.MaKM VALUES (N'[TEST] Storefront', 'FLASH_SALE', DATEADD(hour,-1,GETDATE()), DATEADD(hour,1,GETDATE()), 1)", Integer.class);
    }
    int product(String name, Integer parent, int stock, int expiryHours, boolean hidden) {
        return jdbc.queryForObject("INSERT INTO SanPham (TenSP, Gia, DonViTinh, SoLuongTon, HanSuDung, MaDanhMuc, MaSPCha, TenLoai, TrangThaiXoa) OUTPUT INSERTED.MaSP VALUES (?,10000,N'gói',?,DATEADD(hour,?,GETDATE()),(SELECT TOP 1 MaDanhMuc FROM DanhMuc),?, ?, ?)", Integer.class, name, stock, expiryHours, parent, parent == null ? null : name, hidden);
    }
    void offer(int id, Integer quota, int sold) {
        jdbc.update("INSERT INTO ChiTietKhuyenMai (MaKM,MaSP,GiaKhuyenMai,PhanTramGiam,SoLuongKhuyenMai,SoLuongDaBan) VALUES (?,?,7000,30,?,?)", campaign,id,quota,sold);
    }
    @Test void onlySellableDiscountsAppearAndMatchCheckoutPrice() {
        int leaf=product("[TEST] Leaf",null,5,5,false); offer(leaf,3,1);
        int parent=product("[TEST] Parent",null,8,5,false); offer(parent,null,0);
        int child=product("[TEST] Child",parent,5,5,false); offer(child,null,0);
        offer(product("[TEST] Expired",null,5,-1,false),null,0);
        offer(product("[TEST] Empty",null,0,5,false),null,0);
        offer(product("[TEST] Hidden",null,5,5,true),null,0);
        offer(product("[TEST] Sold out",null,5,5,false),2,2);
        int hiddenParent=product("[TEST] Hidden parent",null,5,5,true);
        offer(product("[TEST] Hidden child",hiddenParent,5,5,false),null,0);
        var result=service.getOffers().stream().filter(c -> c.maKM().equals(campaign)).findFirst().orElseThrow();
        assertEquals(java.util.Set.of(leaf,child), result.products().stream().map(p -> p.getMaSP()).collect(java.util.stream.Collectors.toSet()));
        result.products().forEach(p -> { assertTrue(p.getDangKhuyenMai()); assertEquals(0,p.getGiaHienTai().compareTo(new java.math.BigDecimal("7000"))); assertEquals("AVAILABLE",p.getTrangThaiBan()); });
    }
    @Test void emptyPausedFutureAndExpiredCampaignsDoNotAppear() {
        assertFalse(service.getOffers().stream().anyMatch(c -> c.maKM().equals(campaign)));
        offer(product("[TEST] Timed",null,5,5,false),null,0);
        jdbc.update("UPDATE ChuongTrinhKhuyenMai SET TrangThai=0 WHERE MaKM=?",campaign);
        assertFalse(service.getOffers().stream().anyMatch(c -> c.maKM().equals(campaign)));
        jdbc.update("UPDATE ChuongTrinhKhuyenMai SET TrangThai=1,NgayBatDau=DATEADD(hour,1,GETDATE()),NgayKetThuc=DATEADD(hour,2,GETDATE()) WHERE MaKM=?",campaign);
        assertFalse(service.getOffers().stream().anyMatch(c -> c.maKM().equals(campaign)));
        jdbc.update("UPDATE ChuongTrinhKhuyenMai SET NgayBatDau=DATEADD(hour,-2,GETDATE()),NgayKetThuc=DATEADD(hour,-1,GETDATE()) WHERE MaKM=?",campaign);
        assertFalse(service.getOffers().stream().anyMatch(c -> c.maKM().equals(campaign)));
    }
}
