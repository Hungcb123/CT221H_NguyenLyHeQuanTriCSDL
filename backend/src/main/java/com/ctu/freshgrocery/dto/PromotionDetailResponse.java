package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết mặt hàng trong chương trình khuyến mãi")
public class PromotionDetailResponse {

    @Schema(description = "Mã sản phẩm", example = "8")
    private Integer maSP;

    @Schema(description = "Tên sản phẩm", example = "Tôm sú tươi sống")
    private String tenSP;

    @Schema(description = "Đơn vị tính", example = "kg")
    private String donViTinh;

    @Schema(description = "Giá gốc niêm yết (VND)", example = "220000.00")
    private BigDecimal giaGoc;

    @Schema(description = "Giá bán khuyến mãi (VND)", example = "176000.00")
    private BigDecimal giaKhuyenMai;

    @Schema(description = "Phần trăm giảm giá (%)", example = "20.00")
    private BigDecimal phanTramGiam;

    @Schema(description = "Số tiền tiết kiệm được so với giá gốc (VND)", example = "44000.00")
    private BigDecimal tienTietKiem;

    @Schema(description = "Số lượng suất khuyến mãi", example = "50")
    private Integer soLuongKhuyenMai;

    @Schema(description = "Số lượng đã bán", example = "2")
    private Integer soLuongDaBan;

    @Schema(description = "Số lượng còn tồn trong kho", example = "20")
    private Integer soLuongTon;

    @Schema(description = "Hạn sử dụng sản phẩm")
    private java.time.LocalDateTime hanSuDung;
}
