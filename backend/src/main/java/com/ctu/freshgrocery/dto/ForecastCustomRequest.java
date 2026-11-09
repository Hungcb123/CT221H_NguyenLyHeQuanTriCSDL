package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu dự báo nhu cầu với các tham số kịch bản tùy chỉnh")
public class ForecastCustomRequest {

    @NotNull(message = "Mã sản phẩm không được để trống")
    @Schema(description = "Mã sản phẩm cần dự báo", example = "1")
    private Integer maSP;

    @Schema(description = "Ngày cần dự báo tiêu thụ (để trống sẽ mặc định là ngày mai)", example = "2026-10-15")
    private LocalDate targetDate;

    @Schema(description = "Giá bán giả lập (VND) (để trống sẽ lấy giá niêm yết hiện tại)", example = "75000.00")
    private BigDecimal unitPrice;

    @Schema(description = "Tồn kho thực tế giả lập (để trống sẽ lấy số lượng tồn trong CSDL)", example = "15")
    private Integer currentStock;

    @Schema(description = "Số ngày còn lại trước khi hết hạn sử dụng (để trống sẽ tự tính theo HanSuDung)", example = "2")
    private Integer daysUntilExpiry;

    @Schema(description = "Sản lượng bán ngày hôm qua (Lag_1) (để trống sẽ tự tính từ lịch sử CSDL)", example = "48.0")
    private Double lag1;

    @Schema(description = "Sản lượng bán cùng ngày tuần trước (Lag_7) (để trống sẽ tự tính từ lịch sử CSDL)", example = "45.0")
    private Double lag7;

    @Schema(description = "Trung bình động sản lượng 7 ngày gần nhất (Rolling_Mean_7)", example = "46.5")
    private Double rollingMean7;

    @Schema(description = "Độ lệch chuẩn sản lượng 7 ngày gần nhất (Rolling_Std_7)", example = "4.2")
    private Double rollingStd7;
}
