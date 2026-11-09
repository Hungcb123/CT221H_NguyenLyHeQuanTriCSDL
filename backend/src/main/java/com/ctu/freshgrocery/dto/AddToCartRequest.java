package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu thêm sản phẩm vào giỏ hàng")
public class AddToCartRequest {

    @NotNull(message = "Mã khách hàng không được để trống")
    @Schema(description = "Mã khách hàng", example = "1")
    private Integer maKH;

    @NotNull(message = "Mã sản phẩm không được để trống")
    @Schema(description = "Mã sản phẩm cần thêm", example = "1")
    private Integer maSP;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng trong giỏ phải từ 1 trở lên")
    @Schema(description = "Số lượng mua", example = "2")
    private Integer soLuong;
}
