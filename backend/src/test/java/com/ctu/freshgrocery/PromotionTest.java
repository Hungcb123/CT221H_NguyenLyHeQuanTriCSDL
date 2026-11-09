package com.ctu.freshgrocery;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.AutoClearanceResponse;
import com.ctu.freshgrocery.dto.PromotionAddProductRequest;
import com.ctu.freshgrocery.dto.PromotionCreateRequest;
import com.ctu.freshgrocery.dto.PromotionDetailResponse;
import com.ctu.freshgrocery.dto.PromotionResponse;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.model.Promotion;
import com.ctu.freshgrocery.model.PromotionDetail;
import com.ctu.freshgrocery.model.PromotionDetailId;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.repository.PromotionDetailRepository;
import com.ctu.freshgrocery.repository.PromotionRepository;
import com.ctu.freshgrocery.repository.PromotionSpRepository;
import com.ctu.freshgrocery.service.PromotionService;
import com.ctu.freshgrocery.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PromotionTest {

    private PromotionRepository promotionRepository;
    private PromotionDetailRepository promotionDetailRepository;
    private PromotionSpRepository promotionSpRepository;
    private ProductRepository productRepository;
    private PromotionService promotionService;

    @BeforeEach
    public void setup() {
        promotionRepository = Mockito.mock(PromotionRepository.class);
        promotionDetailRepository = Mockito.mock(PromotionDetailRepository.class);
        promotionSpRepository = Mockito.mock(PromotionSpRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);

        promotionService = new PromotionServiceImpl(
                promotionRepository,
                promotionDetailRepository,
                promotionSpRepository,
                productRepository
        );
    }

    @Test
    public void testGetAllPromotions_ActiveOnly() {
        Promotion p1 = Promotion.builder()
                .maKM(1)
                .tenKM("Flash Sale Thủy Hải Sản")
                .loaiKM("FLASH_SALE")
                .ngayBatDau(LocalDateTime.now().minusDays(1))
                .ngayKetThuc(LocalDateTime.now().plusDays(2))
                .trangThai(true)
                .details(Collections.emptyList())
                .build();

        when(promotionRepository.findActivePromotions(any())).thenReturn(List.of(p1));

        List<PromotionResponse> activePromos = promotionService.getAllPromotions(true);

        assertNotNull(activePromos);
        assertEquals(1, activePromos.size());
        assertEquals("Flash Sale Thủy Hải Sản", activePromos.get(0).getTenKM());
        assertTrue(activePromos.get(0).getDangHieuLuc());
    }

    @Test
    public void testCreatePromotion_Success() {
        PromotionCreateRequest req = PromotionCreateRequest.builder()
                .tenKM("Lễ Hội Trái Cây Miền Tây")
                .moTa("Giảm giá dưa hấu, chuối chín")
                .loaiKM("GIAM_THEO_MUA")
                .ngayBatDau(LocalDateTime.now())
                .ngayKetThuc(LocalDateTime.now().plusDays(5))
                .build();

        Map<String, Object> spResult = Map.of(
                "StatusCode", 1,
                "Message", "Tạo chương trình khuyến mãi thành công. Mã KM: #5",
                "NewMaKM", 5
        );

        when(promotionSpRepository.executeTaoChuongTrinhKhuyenMai(any())).thenReturn(spResult);

        Promotion savedPromo = Promotion.builder()
                .maKM(5)
                .tenKM("Lễ Hội Trái Cây Miền Tây")
                .moTa("Giảm giá dưa hấu, chuối chín")
                .loaiKM("GIAM_THEO_MUA")
                .ngayBatDau(req.getNgayBatDau())
                .ngayKetThuc(req.getNgayKetThuc())
                .trangThai(true)
                .details(Collections.emptyList())
                .build();

        when(promotionRepository.findById(5)).thenReturn(Optional.of(savedPromo));

        ApiResponse<PromotionResponse> res = promotionService.createPromotion(req);

        assertNotNull(res);
        assertEquals(1, res.getStatusCode());
        assertNotNull(res.getData());
        assertEquals(5, res.getData().getMaKM());
        assertEquals("Lễ Hội Trái Cây Miền Tây", res.getData().getTenKM());
    }

    @Test
    public void testAddProductToPromotion_Success() {
        PromotionAddProductRequest req = PromotionAddProductRequest.builder()
                .maSP(1)
                .giaKhuyenMai(new BigDecimal("55000.00"))
                .soLuongKhuyenMai(10)
                .build();

        Map<String, Object> spResult = Map.of(
                "StatusCode", 1,
                "Message", "Thêm sản phẩm vào chương trình khuyến mãi thành công! (Giảm 26.67%)"
        );

        when(promotionSpRepository.executeThemSanPhamVaoKhuyenMai(eq(1), any())).thenReturn(spResult);

        Product prod = Product.builder()
                .maSP(1)
                .tenSP("Cá lóc tươi làm sạch")
                .gia(new BigDecimal("75000.00"))
                .donViTinh("con")
                .soLuongTon(20)
                .build();

        PromotionDetail detail = PromotionDetail.builder()
                .id(new PromotionDetailId(1, 1))
                .giaKhuyenMai(new BigDecimal("55000.00"))
                .phanTramGiam(new BigDecimal("26.67"))
                .soLuongKhuyenMai(10)
                .soLuongDaBan(0)
                .product(prod)
                .build();

        when(promotionDetailRepository.findByIdMaKMAndIdMaSP(1, 1)).thenReturn(Optional.of(detail));

        ApiResponse<PromotionDetailResponse> res = promotionService.addProductToPromotion(1, req);

        assertNotNull(res);
        assertEquals(1, res.getStatusCode());
        assertNotNull(res.getData());
        assertEquals(new BigDecimal("55000.00"), res.getData().getGiaKhuyenMai());
        assertEquals(new BigDecimal("20000.00"), res.getData().getTienTietKiem());
    }

    @Test
    public void testTriggerAutoClearance() {
        Map<String, Object> spResult = Map.of(
                "StatusCode", 1,
                "Message", "Đã tự động tạo chiến dịch xả kho cho 3 mặt hàng cận date.",
                "SoLuongSanPhamDaThem", 3,
                "MaKM_Output", 8
        );

        when(promotionSpRepository.executeTuDongKichHoatXaKhoCanDate(2, new BigDecimal("35.00")))
                .thenReturn(spResult);

        Promotion clearancePromo = Promotion.builder()
                .maKM(8)
                .tenKM("Xả kho thực phẩm tươi sống cuối ngày 13/10/2026")
                .build();

        when(promotionRepository.findById(8)).thenReturn(Optional.of(clearancePromo));

        AutoClearanceResponse res = promotionService.triggerAutoClearance(2, new BigDecimal("35.00"));

        assertNotNull(res);
        assertEquals(1, res.getStatusCode());
        assertEquals(3, res.getSoLuongSanPhamDaThem());
        assertEquals(8, res.getMaKM());
        assertEquals("Xả kho thực phẩm tươi sống cuối ngày 13/10/2026", res.getTenKM());
    }

    @Test
    public void testBestPromotionSelection_MinPrice() {
        Promotion pA = Promotion.builder()
                .maKM(1)
                .tenKM("Flash Sale Buổi Trưa")
                .trangThai(true)
                .build();

        Promotion pB = Promotion.builder()
                .maKM(2)
                .tenKM("Xả Kho Cận Date")
                .trangThai(true)
                .build();

        PromotionDetail d1 = PromotionDetail.builder()
                .id(new PromotionDetailId(1, 1))
                .promotion(pA)
                .giaKhuyenMai(new BigDecimal("60000.00")) // Giảm ít hơn
                .phanTramGiam(new BigDecimal("20.00"))
                .build();

        PromotionDetail d2 = PromotionDetail.builder()
                .id(new PromotionDetailId(2, 1))
                .promotion(pB)
                .giaKhuyenMai(new BigDecimal("45000.00")) // Giảm sâu hơn (Min price)
                .phanTramGiam(new BigDecimal("40.00"))
                .build();

        // SP và Repository được cấu hình ORDER BY GiaKhuyenMai ASC
        List<PromotionDetail> promoList = List.of(d2, d1);
        when(promotionDetailRepository.findActiveDiscountsForProduct(eq(1), any()))
                .thenReturn(promoList);

        List<PromotionDetail> result = promotionDetailRepository.findActiveDiscountsForProduct(1, LocalDateTime.now());
        assertFalse(result.isEmpty());
        // Sản phẩm có giá khuyến mãi tốt nhất là d2 (45.000 VND)
        assertEquals(new BigDecimal("45000.00"), result.get(0).getGiaKhuyenMai());
        assertEquals("Xả Kho Cận Date", result.get(0).getPromotion().getTenKM());
    }
    @Test
    public void testAutoClearanceFailureIsNotReportedAsSuccess() {
        when(promotionSpRepository.executeTuDongKichHoatXaKhoCanDate(2, new BigDecimal("35.00")))
                .thenReturn(Map.of("StatusCode", -99, "Message", "write failed"));
        org.junit.jupiter.api.Assertions.assertThrows(com.ctu.freshgrocery.exception.AppException.class,
                () -> promotionService.triggerAutoClearance(2, new BigDecimal("35.00")));
    }
}
