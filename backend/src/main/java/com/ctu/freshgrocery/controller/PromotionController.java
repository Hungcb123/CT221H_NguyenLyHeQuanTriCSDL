package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.AutoClearanceResponse;
import com.ctu.freshgrocery.dto.PromotionAddProductRequest;
import com.ctu.freshgrocery.dto.PromotionCreateRequest;
import com.ctu.freshgrocery.dto.PromotionDetailResponse;
import com.ctu.freshgrocery.dto.PromotionResponse;
import com.ctu.freshgrocery.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotion & Flash Sale Management", description = "Quản lý chương trình khuyến mãi, Flash Sale và tự động xả kho thực phẩm cận date (3NF RDBMS)")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @Operation(summary = "Lấy danh sách chương trình khuyến mãi", description = "Truy vấn danh sách tất cả chương trình khuyến mãi hoặc chỉ các chương trình đang còn hiệu lực")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getAllPromotions(
            @Parameter(description = "Chỉ lấy các chương trình đang có hiệu lực thời gian và trạng thái kích hoạt")
            @RequestParam(name = "activeOnly", required = false, defaultValue = "false") Boolean activeOnly) {
        List<PromotionResponse> list = promotionService.getAllPromotions(activeOnly);
        return ResponseEntity.ok(ApiResponse.ok(1, "Lấy danh sách chương trình khuyến mãi thành công", list));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết chương trình khuyến mãi", description = "Truy vấn chi tiết chương trình khuyến mãi và các mặt hàng giảm giá đang áp dụng")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotionById(
            @Parameter(description = "Mã chương trình khuyến mãi", example = "1")
            @PathVariable("id") Integer id) {
        PromotionResponse promotion = promotionService.getPromotionById(id);
        return ResponseEntity.ok(ApiResponse.ok(1, "Lấy chi tiết chương trình khuyến mãi thành công", promotion));
    }

    @PostMapping
    @Operation(summary = "Tạo mới chương trình khuyến mãi", description = "Thực thi Stored Procedure `sp_TaoChuongTrinhKhuyenMai` để khởi tạo chiến dịch khuyến mãi hoặc Flash Sale mới")
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(
            @Valid @RequestBody PromotionCreateRequest request) {
        ApiResponse<PromotionResponse> response = promotionService.createPromotion(request);
        if (response.getStatusCode() == 1) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/products")
    @Operation(summary = "Thêm sản phẩm vào chương trình khuyến mãi", description = "Thực thi Stored Procedure `sp_ThemSanPhamVaoKhuyenMai`. Ràng buộc kiểm soát: Giá KM phải nhỏ hơn Giá gốc, tự động tính % giảm.")
    public ResponseEntity<ApiResponse<PromotionDetailResponse>> addProductToPromotion(
            @Parameter(description = "Mã chương trình khuyến mãi", example = "1")
            @PathVariable("id") Integer id,
            @Valid @RequestBody PromotionAddProductRequest request) {
        ApiResponse<PromotionDetailResponse> response = promotionService.addProductToPromotion(id, request);
        if (response.getStatusCode() == 1) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/auto-clearance")
    @Operation(summary = "Tự động kích hoạt xả kho thực phẩm cận date", description = "Thực thi Stored Procedure `sp_TuDongKichHoatXaKhoCanDate`. Tự động quét thực phẩm có HSD <= số ngày quy định và đưa vào chiến dịch xả kho giảm giá.")
    public ResponseEntity<ApiResponse<AutoClearanceResponse>> triggerAutoClearance(
            @Parameter(description = "Số ngày cận hạn sử dụng (mặc định: 2 ngày)", example = "2")
            @RequestParam(name = "soNgayCanDate", required = false, defaultValue = "2") Integer soNgayCanDate,
            @Parameter(description = "Phần trăm giảm giá đề xuất (mặc định: 30.00%)", example = "30.00")
            @RequestParam(name = "phanTramGiam", required = false, defaultValue = "30.00") BigDecimal phanTramGiam) {
        AutoClearanceResponse response = promotionService.triggerAutoClearance(soNgayCanDate, phanTramGiam);
        return ResponseEntity.ok(ApiResponse.ok(1, response.getMessage(), response));
    }
}
