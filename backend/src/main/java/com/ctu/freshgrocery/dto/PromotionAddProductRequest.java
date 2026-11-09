package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu thêm sản phẩm vào chương trình khuyến mãi")
public class PromotionAddProductRequest {

    @NotNull(message = "Mã sản phẩm không được để trống")
    @Schema(description = "Mã sản phẩm cần giảm giá", example = "8")
    private Integer maSP;

    @NotNull(message = "Giá khuyến mãi không được để trống")
    @DecimalMin(value = "1.0", message = "Giá khuyến mãi phải lớn hơn 0")
    @Schema(description = "Giá bán sau giảm (VND) - Bắt buộc phải nhỏ hơn giá gốc", example = "176000.00")
    private BigDecimal giaKhuyenMai;

    @Schema(description = "Số lượng suất khuyến mãi giới hạn (để trống nếu không giới hạn)", example = "50")
    @jakarta.validation.constraints.Min(1)
    private Integer soLuongKhuyenMai;
}
