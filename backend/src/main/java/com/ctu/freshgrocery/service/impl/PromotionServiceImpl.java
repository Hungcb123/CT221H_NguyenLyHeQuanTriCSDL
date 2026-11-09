package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.AutoClearanceResponse;
import com.ctu.freshgrocery.dto.PromotionAddProductRequest;
import com.ctu.freshgrocery.dto.PromotionCreateRequest;
import com.ctu.freshgrocery.dto.PromotionDetailResponse;
import com.ctu.freshgrocery.dto.PromotionResponse;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.model.Promotion;
import com.ctu.freshgrocery.model.PromotionDetail;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.repository.PromotionDetailRepository;
import com.ctu.freshgrocery.repository.PromotionRepository;
import com.ctu.freshgrocery.repository.PromotionSpRepository;
import com.ctu.freshgrocery.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionDetailRepository promotionDetailRepository;
    private final PromotionSpRepository promotionSpRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponse> getAllPromotions(Boolean activeOnly) {
        List<Promotion> promotions;
        if (Boolean.TRUE.equals(activeOnly)) {
            promotions = promotionRepository.findActivePromotions(LocalDateTime.now());
        } else {
            promotions = promotionRepository.findAll();
        }

        return promotions.stream()
                .map(this::mapToPromotionSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionById(Integer id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy chương trình khuyến mãi với ID: " + id));

        return mapToPromotionDetail(promotion);
    }

    @Override
    public ApiResponse<PromotionResponse> createPromotion(PromotionCreateRequest request) {
        log.info("Tạo chương trình khuyến mãi mới: {}", request.getTenKM());

        Map<String, Object> result = promotionSpRepository.executeTaoChuongTrinhKhuyenMai(request);
        Integer statusCode = (Integer) result.get("StatusCode");
        String message = (String) result.get("Message");
        Integer newMaKM = (Integer) result.get("NewMaKM");

        if (statusCode == null || statusCode != 1) {
            log.warn("Thủ tục sp_TaoChuongTrinhKhuyenMai thất bại: code={}, message={}", statusCode, message);
            return ApiResponse.error(statusCode != null ? statusCode : -99, message);
        }

        Promotion saved = promotionRepository.findById(newMaKM).orElse(null);
        PromotionResponse resp = saved != null ? mapToPromotionDetail(saved) : PromotionResponse.builder().maKM(newMaKM).build();

        return ApiResponse.ok(1, message, resp);
    }

    @Override
    public ApiResponse<PromotionDetailResponse> addProductToPromotion(Integer promotionId, PromotionAddProductRequest request) {
        log.info("Thêm sản phẩm #{} vào chương trình KM #{} với giá KM: {}",
                request.getMaSP(), promotionId, request.getGiaKhuyenMai());

        Map<String, Object> result = promotionSpRepository.executeThemSanPhamVaoKhuyenMai(promotionId, request);
        Integer statusCode = (Integer) result.get("StatusCode");
        String message = (String) result.get("Message");

        if (statusCode == null || statusCode != 1) {
            log.warn("Thủ tục sp_ThemSanPhamVaoKhuyenMai thất bại: code={}, message={}", statusCode, message);
            return ApiResponse.error(statusCode != null ? statusCode : -99, message);
        }

        PromotionDetail detail = promotionDetailRepository.findByIdMaKMAndIdMaSP(promotionId, request.getMaSP()).orElse(null);
        PromotionDetailResponse resp = detail != null ? mapToPromotionDetailItem(detail) : null;

        return ApiResponse.ok(1, message, resp);
    }

    @Override
    public AutoClearanceResponse triggerAutoClearance(Integer soNgayCanDate, BigDecimal phanTramGiam) {
        int days = soNgayCanDate != null ? soNgayCanDate : 2;
        BigDecimal discountPercent = phanTramGiam != null ? phanTramGiam : new BigDecimal("30.00");

        log.info("Kích hoạt tự động xả kho thực phẩm cận date (<= {} ngày, giảm {}%)", days, discountPercent);

        Map<String, Object> result = promotionSpRepository.executeTuDongKichHoatXaKhoCanDate(days, discountPercent);
        Integer statusCode = (Integer) result.get("StatusCode");
        String message = (String) result.get("Message");
        if (statusCode == null || statusCode != 1) {
            throw new com.ctu.freshgrocery.exception.AppException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    message != null ? message : "Không kích hoạt được xả kho cận hạn.");
        }
        Integer addedCount = (Integer) result.get("SoLuongSanPhamDaThem");
        Integer maKM = (Integer) result.get("MaKM_Output");

        String tenKM = "";
        if (maKM != null && maKM > 0) {
            tenKM = promotionRepository.findById(maKM)
                    .map(Promotion::getTenKM)
                    .orElse("Chiến dịch xả kho cận date #" + maKM);
        }

        return AutoClearanceResponse.builder()
                .maKM(maKM)
                .tenKM(tenKM)
                .soLuongSanPhamDaThem(addedCount != null ? addedCount : 0)
                .phanTramGiam(discountPercent)
                .statusCode(statusCode != null ? statusCode : 1)
                .message(message != null ? message : "Đã kích hoạt tự động xả kho cận date thành công")
                .build();
    }

    private PromotionResponse mapToPromotionSummary(Promotion p) {
        LocalDateTime now = LocalDateTime.now();
        boolean isActive = Boolean.TRUE.equals(p.getTrangThai())
                && !now.isBefore(p.getNgayBatDau())
                && !now.isAfter(p.getNgayKetThuc());

        int count = p.getChiTietKhuyenMai() != null ? p.getChiTietKhuyenMai().size() : 0;

        return PromotionResponse.builder()
                .maKM(p.getMaKM())
                .tenKM(p.getTenKM())
                .moTa(p.getMoTa())
                .loaiKM(p.getLoaiKM())
                .ngayBatDau(p.getNgayBatDau())
                .ngayKetThuc(p.getNgayKetThuc())
                .trangThai(p.getTrangThai())
                .dangHieuLuc(isActive)
                .soLuongSanPham(count)
                .items(Collections.emptyList())
                .build();
    }

    private PromotionResponse mapToPromotionDetail(Promotion p) {
        LocalDateTime now = LocalDateTime.now();
        boolean isActive = Boolean.TRUE.equals(p.getTrangThai())
                && !now.isBefore(p.getNgayBatDau())
                && !now.isAfter(p.getNgayKetThuc());

        List<PromotionDetailResponse> itemResponses = Collections.emptyList();
        if (p.getChiTietKhuyenMai() != null) {
            itemResponses = p.getChiTietKhuyenMai().stream()
                    .map(this::mapToPromotionDetailItem)
                    .collect(Collectors.toList());
        }

        return PromotionResponse.builder()
                .maKM(p.getMaKM())
                .tenKM(p.getTenKM())
                .moTa(p.getMoTa())
                .loaiKM(p.getLoaiKM())
                .ngayBatDau(p.getNgayBatDau())
                .ngayKetThuc(p.getNgayKetThuc())
                .trangThai(p.getTrangThai())
                .dangHieuLuc(isActive)
                .soLuongSanPham(itemResponses.size())
                .items(itemResponses)
                .build();
    }

    private PromotionDetailResponse mapToPromotionDetailItem(PromotionDetail item) {
        Product prod = item.getProduct();
        BigDecimal giaGoc = prod != null ? prod.getGia() : BigDecimal.ZERO;
        String tenSP = prod != null ? prod.getTenSP() : "Sản phẩm #" + item.getId().getMaSP();
        String donViTinh = prod != null ? prod.getDonViTinh() : "";
        Integer tonKho = prod != null ? prod.getSoLuongTon() : 0;
        LocalDateTime hsd = prod != null ? prod.getHanSuDung() : null;

        BigDecimal tienTietKiem = giaGoc.subtract(item.getGiaKhuyenMai());
        if (tienTietKiem.compareTo(BigDecimal.ZERO) < 0) {
            tienTietKiem = BigDecimal.ZERO;
        }

        return PromotionDetailResponse.builder()
                .maSP(item.getId().getMaSP())
                .tenSP(tenSP)
                .donViTinh(donViTinh)
                .giaGoc(giaGoc)
                .giaKhuyenMai(item.getGiaKhuyenMai())
                .phanTramGiam(item.getPhanTramGiam())
                .tienTietKiem(tienTietKiem)
                .soLuongKhuyenMai(item.getSoLuongKhuyenMai())
                .soLuongDaBan(item.getSoLuongDaBan())
                .soLuongTon(tonKho)
                .hanSuDung(hsd)
                .build();
    }
}
