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
@Schema(description = "Kết quả kích hoạt tự động chiến dịch xả kho thực phẩm cận date")
public class AutoClearanceResponse {

    @Schema(description = "Mã chương trình khuyến mãi được tạo/sử dụng", example = "3")
    private Integer maKM;

    @Schema(description = "Tên chương trình khuyến mãi", example = "Xả kho thực phẩm tươi sống cuối ngày 13/10/2026")
    private String tenKM;

    @Schema(description = "Số lượng mặt hàng cận hạn sử dụng đã được tự động thêm vào giảm giá", example = "4")
    private Integer soLuongSanPhamDaThem;

    @Schema(description = "Phần trăm giảm giá đề xuất", example = "35.00")
    private BigDecimal phanTramGiam;

    @Schema(description = "Mã trạng thái từ Stored Procedure", example = "1")
    private Integer statusCode;

    @Schema(description = "Thông báo kết quả", example = "Đã tự động tạo chương trình xả kho và áp dụng giảm giá cho 4 sản phẩm cận date.")
    private String message;
}
