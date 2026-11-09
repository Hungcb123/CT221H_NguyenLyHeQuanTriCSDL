package com.ctu.freshgrocery;

import com.ctu.freshgrocery.model.*;
import com.ctu.freshgrocery.repository.*;
import com.ctu.freshgrocery.service.impl.RecipeServiceImpl;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeAvailabilityTest {
    private final RecipeRepository repository = mock(RecipeRepository.class);
    private final ProductRepository catalog = mock(ProductRepository.class);
    private final RecipeServiceImpl service = new RecipeServiceImpl(repository,
            mock(RecipeSpRepository.class), mock(CustomerRepository.class), catalog,
            mock(com.ctu.freshgrocery.service.CartService.class), mock(org.springframework.jdbc.core.JdbcTemplate.class));

    private Product product() {
        return Product.builder().maSP(1).tenSP("Ca loc").gia(BigDecimal.TEN)
                .soLuongTon(25).hanSuDung(LocalDateTime.now().plusDays(2)).trangThaiXoa(false).build();
    }

    private com.ctu.freshgrocery.dto.RecipeDetailDto detail(Product product) {
        var recipe = Recipe.builder().maCongThuc(1).tenMonAn("Canh chua").khauPhanMacDinh(4)
                .ingredients(List.of(RecipeIngredient.builder().product(product)
                        .soLuongDinhLuong(BigDecimal.ONE).donViTinh("con").build())).build();
        when(repository.findById(1)).thenReturn(Optional.of(recipe));
        return service.getRecipeDetail(1, 4);
    }

    @Test void expiredProductWithStockCannotBePurchased() {
        var product = product(); product.setHanSuDung(LocalDateTime.now().minusDays(1));
        var result = detail(product);
        assertFalse(result.getDuNguyenLieuTrongKho());
        assertFalse(result.getNguyenLieuList().getFirst().getConHang());
        assertNotNull(result.getNguyenLieuList().getFirst().getLyDoKhongKhaDung());
    }
    @Test void activeFreshProductIsAvailable() {
        var result = detail(product());
        assertTrue(result.getDuNguyenLieuTrongKho());
        assertNull(result.getNguyenLieuList().getFirst().getLyDoKhongKhaDung());
    }
    @Test void hiddenOrInsufficientProductIsUnavailable() {
        var product = product(); product.setTrangThaiXoa(true);
        assertFalse(detail(product).getDuNguyenLieuTrongKho());
        product.setTrangThaiXoa(false); product.setSoLuongTon(0);
        assertFalse(detail(product).getDuNguyenLieuTrongKho());
    }
    @Test void resolvesAvailableReplacementFromLiveCatalogInsteadOfFixedSku() {
        var old = product(); old.setHanSuDung(LocalDateTime.now().minusDays(2));
        var fresh = product(); fresh.setMaSP(83); fresh.setTenSP("Ca loc moi");
        when(catalog.findAll()).thenReturn(List.of(old, fresh));
        var result = detail(old);
        assertTrue(result.getDuNguyenLieuTrongKho());
        assertEquals(83, result.getNguyenLieuList().getFirst().getMaSP());
        fresh.setSoLuongTon(0);
        assertFalse(detail(old).getDuNguyenLieuTrongKho());
    }
    @Test void refusesDifferentIngredientAndIncompatibleSalesUnit() {
        var old = product(); old.setDonViTinh("con"); old.setHanSuDung(LocalDateTime.now().minusDays(2));
        var other = product(); other.setMaSP(83); other.setTenSP("Ca hoi"); other.setDonViTinh("con");
        when(catalog.findAll()).thenReturn(List.of(other));
        assertFalse(detail(old).getDuNguyenLieuTrongKho());
        other.setTenSP("Ca loc moi"); other.setDonViTinh("kg");
        assertFalse(detail(old).getDuNguyenLieuTrongKho());
    }
}
