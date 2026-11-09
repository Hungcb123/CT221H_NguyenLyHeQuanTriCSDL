package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

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
    @Digits(integer = 16, fraction = 2, message = "Số tiền nạp vượt quá định dạng cho phép")
    @Schema(description = "Số tiền cần nạp (VND)", example = "200000.00")
    private BigDecimal soTien;

    @Size(max = 120, message = "Ghi chú nạp ví tối đa 120 ký tự")
    @Schema(description = "Nội dung giao dịch nạp tiền", example = "Nạp tiền ví Bách Hóa Xanh")
    private String noiDung;

    @Schema(description = "Mã yêu cầu để gửi lại an toàn khi mất kết nối")
    private UUID maYeuCau;
}
