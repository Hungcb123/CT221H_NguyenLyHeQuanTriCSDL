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
@Schema(description = "Thông tin phản hồi số dư ví")
public class WalletResponse {

    @Schema(description = "Mã khách hàng", example = "1")
    private Integer maKH;

    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn An")
    private String hoTen;

    @Schema(description = "Số dư hiện tại của ví (VND)", example = "1000000.00")
    private BigDecimal soDuVi;
}
