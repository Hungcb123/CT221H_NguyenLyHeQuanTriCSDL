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
@Schema(description = "Kết quả thanh toán đơn hàng")
public class CheckoutResponse {

    @Schema(description = "Mã đơn hàng tạo thành công", example = "1")
    private Integer maDH;

    @Schema(description = "Trạng thái kết quả (1: Thành công, <0: Thất bại)", example = "1")
    private Integer statusCode;

    @Schema(description = "Thông báo kết quả từ Stored Procedure", example = "Thanh toán đơn hàng thành công qua ví điện tử.")
    private String message;

    @Schema(description = "Tổng tiền thanh toán", example = "182000.00")
    private BigDecimal tongTien;

    @Schema(description = "Số dư ví còn lại sau thanh toán", example = "818000.00")
    private BigDecimal soDuConLai;
}
