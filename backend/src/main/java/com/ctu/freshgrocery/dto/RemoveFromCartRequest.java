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
@Schema(description = "Yêu cầu xóa sản phẩm khỏi giỏ hàng")
public class RemoveFromCartRequest {

    @NotNull(message = "Mã khách hàng không được để trống")
    @Schema(description = "Mã khách hàng", example = "1")
    private Integer maKH;

    @NotNull(message = "Mã sản phẩm không được để trống")
    @Schema(description = "Mã sản phẩm cần xóa", example = "1")
    private Integer maSP;
}
