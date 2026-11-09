package com.ctu.freshgrocery;

import com.ctu.freshgrocery.dto.ProductResponse;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.repository.*;
import com.ctu.freshgrocery.service.SemanticSearchService;
import com.ctu.freshgrocery.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductKeywordSearchTest {
    ProductRepository repository = mock(ProductRepository.class);
    ProductSpRepository procedures = mock(ProductSpRepository.class);
    SemanticSearchService semantic = mock(SemanticSearchService.class);
    ProductServiceImpl service = new ProductServiceImpl(procedures, repository, semantic);

    void catalog() {
        when(repository.findAll()).thenReturn(List.of(
                Product.builder().maSP(1).tenSP("Cá lóc").trangThaiXoa(false).build(),
                Product.builder().maSP(2).tenSP("Cá hồi").trangThaiXoa(false).build(),
                Product.builder().maSP(3).tenSP("Thịt bò").trangThaiXoa(false).build()));
        for (int id = 1; id <= 2; id++) {
            when(procedures.callLayChiTietSanPham(id)).thenReturn(Optional.of(ProductResponse.builder()
                    .maSP(id).tenSP(id == 1 ? "Cá lóc" : "Cá hồi").maDanhMuc(3)
                    .giaHienTai(BigDecimal.valueOf(id * 10)).soLuongTon(2).build()));
        }
    }

    @Test
    void keywordSearchUsesBm25AndPreservesPaginationWithoutCallingLikeProcedure() {
        catalog();
        var first = service.getProducts(null, "ca", null, null, null, 1, 1).getData();
        var second = service.getProducts(null, "ca", null, null, null, 2, 1).getData();
        assertEquals(2, first.getTotalRecords());
        assertEquals(2, first.getTotalPages());
        assertTrue(first.isHasNext());
        assertEquals(1, first.getItems().getFirst().getMaSP());
        assertEquals(2, second.getItems().getFirst().getMaSP());
        verify(procedures, never()).callLayDanhSachSanPhamPhanTrang(any(), any(), any(), any(), any(), anyInt(), anyInt());
        verifyNoInteractions(semantic);
    }

    @Test
    void priceAndCategoryFiltersApplyToRealHydratedPrices() {
        catalog();
        var response = service.getProducts(3, "ca", BigDecimal.valueOf(15), null, "GIA_ASC", 1, 12).getData();
        assertEquals(1, response.getTotalRecords());
        assertEquals(2, response.getItems().getFirst().getMaSP());
        assertTrue(service.getProducts(9, "ca", null, null, null, 1, 12).getData().getItems().isEmpty());
    }

    @Test
    void explicitPriceAndExpirySortsArePreserved() {
        catalog();
        for (int id = 1; id <= 2; id++) {
            when(procedures.callLayChiTietSanPham(id)).thenReturn(Optional.of(ProductResponse.builder()
                    .maSP(id).tenSP(id == 1 ? "Cá lóc" : "Cá hồi").maDanhMuc(3)
                    .giaHienTai(BigDecimal.valueOf(id * 10)).soLuongTon(2)
                    .hanSuDung(java.time.LocalDateTime.of(2031 - id, 1, 1, 0, 0)).build()));
        }
        assertEquals(2, service.getProducts(null, "ca", null, null, "GIA_DESC", 1, 12)
                .getData().getItems().getFirst().getMaSP());
        assertEquals(2, service.getProducts(null, "ca", null, null, "HSD_ASC", 1, 12)
                .getData().getItems().getFirst().getMaSP());
    }

    @Test
    void missingTypeDoesNotReturnPartialKeywordOverlaps() {
        catalog();
        assertTrue(service.getProducts(null, "thịt gà nguyên con", null, null, null, 1, 12)
                .getData().getItems().isEmpty());
    }
}
