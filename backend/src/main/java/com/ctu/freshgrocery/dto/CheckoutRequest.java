package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu thanh toán đơn hàng bằng ví điện tử")
public class CheckoutRequest {

    @NotNull(message = "Mã khách hàng không được để trống")
    @Schema(description = "Mã khách hàng thanh toán", example = "1")
    private Integer maKH;

    @Schema(description = "Địa chỉ giao hàng (để trống nếu dùng địa chỉ mặc định trong hồ sơ)", example = "123 Đường 3/2, P. Xuân Khánh, Q. Ninh Kiều, Cần Thơ")
    private String diaChiGiaoHang;

    @Schema(description = "Ghi chú giao hàng", example = "Giao buổi sáng trước 11h giúp mình nhé")
    private String ghiChu;
}
