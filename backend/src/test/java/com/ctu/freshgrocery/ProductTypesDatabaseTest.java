package com.ctu.freshgrocery;

import com.ctu.freshgrocery.controller.ProductTypeController;
import com.ctu.freshgrocery.dto.*;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.repository.*;
import com.ctu.freshgrocery.service.*;
import com.ctu.freshgrocery.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ProductTypeService.class, ProductSpRepository.class, ProductServiceImpl.class, ProductTypeController.class, OrderSpRepository.class})
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
class ProductTypesDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductServiceImpl service;
    @Autowired ProductTypeController controller;
    @Autowired OrderSpRepository cart;
    @Autowired PlatformTransactionManager transactions;
    @MockBean ProductRepository repository;
    @MockBean SemanticSearchService semantic;
    int category;

    @org.springframework.test.context.transaction.BeforeTransaction
    void migrate() throws Exception {
        jdbc.execute(new ClassPathResource("product-types.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    @BeforeEach void setup() {
        category = jdbc.queryForObject("SELECT TOP 1 MaDanhMuc FROM DanhMuc ORDER BY MaDanhMuc", Integer.class);
    }
    ProductTypeRequest type(String name, int price, int stock) {
        return ProductTypeRequest.builder().tenLoai(name).gia(BigDecimal.valueOf(price))
                .donViTinh("kg").soLuongTon(stock).hanSuDung(LocalDateTime.now().plusDays(5)).build();
    }
    ProductCreateRequest product(String name, List<ProductTypeRequest> types) {
        return ProductCreateRequest.builder().tenSP(name).gia(BigDecimal.valueOf(100000))
                .donViTinh("kg").soLuongTon(10).hanSuDung(LocalDateTime.now().plusDays(5))
                .maDanhMuc(category).cacLoai(types).build();
    }
    ProductTypeRequest existing(ProductResponse p) {
        return ProductTypeRequest.builder().maSP(p.getMaSP()).tenLoai(p.getTenLoai())
                .gia(p.getGiaGoc()).donViTinh(p.getDonViTinh()).soLuongTon(p.getSoLuongTon())
                .hanSuDung(p.getHanSuDung()).build();
    }

    @Test void createProductWithMultipleTypesInOneRequestAndKeepOtherProductsSeparate() {
        var parent = service.createProduct(product("QA A", List.of(type("Leg", 100000, 8), type("Lean", 120000, 12)))).getData();
        var children = controller.types(parent.getMaSP()).getData();
        assertEquals(2, children.size());
        assertTrue(children.stream().allMatch(p -> parent.getMaSP().equals(p.getMaSPCha())));
        assertNotEquals(children.get(0).getMaSP(), children.get(1).getMaSP());
        assertEquals(0, BigDecimal.valueOf(120000).compareTo(children.get(1).getGiaHienTai()));
        assertEquals(12, children.get(1).getSoLuongTon());
        var another = service.createProduct(product("QA B", List.of())).getData();
        assertEquals(List.of(another.getMaSP()), controller.types(another.getMaSP()).getData().stream().map(ProductResponse::getMaSP).toList());
        assertEquals(children.size(), controller.types(children.getFirst().getMaSP()).getData().size());
    }

    @Test void editProductCanAddRenameAndHideTypesWithoutChangingExistingIds() {
        var parent = service.createProduct(product("QA A", List.of(type("Old", 100000, 8)))).getData();
        var first = controller.adminTypes(parent.getMaSP()).getData().getFirst();
        var updated = existing(first); updated.setTenLoai("Renamed"); updated.setGia(BigDecimal.valueOf(125000));
        service.updateProduct(parent.getMaSP(), ProductUpdateRequest.builder().cacLoai(List.of(updated, type("New", 90000, 3))).build());
        var children = controller.adminTypes(parent.getMaSP()).getData();
        assertEquals(2, children.size());
        assertEquals(first.getMaSP(), children.getFirst().getMaSP());
        assertEquals("Renamed", children.getFirst().getTenLoai());
        var hidden = existing(children.getFirst()); hidden.setTrangThaiXoa(true);
        service.updateProduct(parent.getMaSP(), ProductUpdateRequest.builder().cacLoai(List.of(hidden, existing(children.get(1)))).build());
        assertEquals(2, controller.adminTypes(parent.getMaSP()).getData().size());
        assertEquals(1, controller.types(parent.getMaSP()).getData().size());
    }

    @Test void invalidChildRollsBackTheParentAndAllTypesTogether() {
        var tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM SanPham", Integer.class);
        assertThrows(AppException.class, () -> tx.execute(status -> service.createProduct(product("QA Rollback",
                List.of(type("Same", 100000, 8), type("same", 110000, 9))))));
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM SanPham", Integer.class));
    }

    @Test void aTypeCannotBeMovedIntoAnotherProductBySubmittingItsId() {
        var a = service.createProduct(product("QA A", List.of(type("Owned", 100000, 8)))).getData();
        var b = service.createProduct(product("QA B", List.of())).getData();
        var stolen = existing(controller.types(a.getMaSP()).getData().getFirst());
        var tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED);
        assertThrows(AppException.class, () -> tx.execute(status -> service.updateProduct(b.getMaSP(),
                ProductUpdateRequest.builder().tenSP("Invalid rename").cacLoai(List.of(stolen)).build())));
        assertEquals("QA B", service.getProductById(b.getMaSP()).getData().getTenSP());
    }

    @Test void paginatedCatalogContainsProductsAndNotTheirTypes() {
        var parent = service.createProduct(product("QA Parent", List.of(type("Child", 100000, 8)))).getData();
        var result = service.getProducts(null, null, null, null, "MOI_NHAT", 1, 100).getData();
        assertTrue(result.getItems().stream().anyMatch(p -> p.getMaSP().equals(parent.getMaSP())));
        assertTrue(result.getItems().stream().allMatch(p -> p.getMaSPCha() == null));
    }

    @Test void cartKeepsTheSelectedTypeIdPriceAndQuantity() {
        var parent = service.createProduct(product("QA Cart", List.of(type("Leg", 100000, 8), type("Lean", 120000, 12)))).getData();
        var children = controller.types(parent.getMaSP()).getData();
        var selected = children.get(1);
        var key = UUID.randomUUID().toString();
        int customer = jdbc.queryForObject("INSERT INTO KhachHang (HoTen, Email, SoDienThoai, SoDuVi) OUTPUT INSERTED.MaKH VALUES ('QA Types', ?, ?, 0)", Integer.class, key + "@example.invalid", key.replace("-", "").substring(0, 15));
        assertEquals(1, cart.executeThemVaoGioHang(customer, selected.getMaSP(), 2, false).get("StatusCode"));
        var item = cart.executeLayGioHangKhachHang(customer).getFirst();
        assertEquals(selected.getMaSP(), item.getMaSP());
        assertEquals(2, item.getSoLuong());
        assertEquals(0, BigDecimal.valueOf(120000).compareTo(item.getDonGia()));
    }
    @Test void expiredStockIsNotSellableAndSqlClockControlsAvailability() {
        var p = service.createProduct(product("QA Expired stock", List.of())).getData();
        jdbc.update("UPDATE SanPham SET HanSuDung = DATEADD(DAY, -1, GETDATE()), SoLuongTon = 45 WHERE MaSP = ?", p.getMaSP());
        var expired = service.getProductById(p.getMaSP()).getData();
        assertEquals(45, expired.getSoLuongTon());
        assertEquals("EXPIRED", expired.getTrangThaiBan());
        jdbc.update("UPDATE SanPham SET HanSuDung = DATEADD(DAY, 1, GETDATE()), SoLuongTon = 0 WHERE MaSP = ?", p.getMaSP());
        assertEquals("OUT_OF_STOCK", service.getProductById(p.getMaSP()).getData().getTrangThaiBan());
        jdbc.update("UPDATE SanPham SET SoLuongTon = 45 WHERE MaSP = ?", p.getMaSP());
        assertEquals("AVAILABLE", service.getProductById(p.getMaSP()).getData().getTrangThaiBan());
    }

    @Test void parentUsesAvailableTypesInsteadOfItsOwnExpiryAndStock() {
        var parent = service.createProduct(product("QA Availability types", List.of(type("Fresh", 100000, 8), type("Old", 120000, 12)))).getData();
        var children = controller.types(parent.getMaSP()).getData();
        jdbc.update("UPDATE SanPham SET HanSuDung = DATEADD(DAY, -1, GETDATE()) WHERE MaSP IN (?, ?)", parent.getMaSP(), children.get(1).getMaSP());
        assertEquals("AVAILABLE", service.getProductById(parent.getMaSP()).getData().getTrangThaiBan());
        assertEquals("EXPIRED", service.getProductById(children.get(1).getMaSP()).getData().getTrangThaiBan());
        jdbc.update("UPDATE SanPham SET SoLuongTon = 0 WHERE MaSP = ?", children.getFirst().getMaSP());
        assertEquals("NO_AVAILABLE_TYPES", service.getProductById(parent.getMaSP()).getData().getTrangThaiBan());
        jdbc.update("UPDATE SanPham SET TrangThaiXoa = 1 WHERE MaSP = ?", parent.getMaSP());
        assertEquals("STOPPED", typesStatus(children.getFirst().getMaSP()));
    }

    private String typesStatus(int id) {
        return service.getProductById(id).getData().getTrangThaiBan();
    }

}
