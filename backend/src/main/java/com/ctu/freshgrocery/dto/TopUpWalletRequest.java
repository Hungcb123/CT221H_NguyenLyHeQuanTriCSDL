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
@Schema(description = "Yêu cầu nạp tiền vào ví điện tử")
public class TopUpWalletRequest {

    @NotNull(message = "Mã khách hàng không được để trống")
    @Schema(description = "Mã khách hàng", example = "1")
    private Integer maKH;

    @NotNull(message = "Số tiền nạp không được để trống")
    @DecimalMin(value = "1000.00", message = "Số tiền nạp tối thiểu là 1.000 VND")
    @Schema(description = "Số tiền cần nạp (VND)", example = "200000.00")
    private BigDecimal soTien;

    @Schema(description = "Nội dung giao dịch nạp tiền", example = "Nạp tiền ví Bách Hóa Xanh")
    private String noiDung;
}
