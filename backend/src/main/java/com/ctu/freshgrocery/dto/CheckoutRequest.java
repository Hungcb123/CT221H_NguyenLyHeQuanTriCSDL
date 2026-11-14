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
@Schema(description = "Yêu cầu đặt hàng COD hoặc thanh toán bằng ví điện tử")
public class CheckoutRequest {

    @NotNull(message = "Mã khách hàng không được để trống")
    @Schema(description = "Mã khách hàng thanh toán", example = "1")
    private Integer maKH;

    @Schema(description = "Địa chỉ giao hàng (để trống nếu dùng địa chỉ mặc định trong hồ sơ)", example = "123 Đường 3/2, P. Xuân Khánh, Q. Ninh Kiều, Cần Thơ")
    private String diaChiGiaoHang;

    @Schema(description = "Ghi chú giao hàng", example = "Giao buổi sáng trước 11h giúp mình nhé")
    private String ghiChu;

    @jakarta.validation.constraints.Pattern(regexp = "WALLET|COD", message = "Chọn COD hoặc WALLET")
    @NotNull
    @Builder.Default
    private String phuongThucThanhToan = "COD";
}
