package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả đối chứng kiểm thử tranh chấp đồng thời (Concurrency Test Demo)")
public class ConcurrencyTestResponse {

    @Schema(description = "Mã sản phẩm kiểm thử tranh chấp (Flash Sale tồn kho = 1)", example = "999")
    private Integer productId;

    @Schema(description = "Tên sản phẩm kiểm thử", example = "Tôm hùm Alaska Flash Sale (Tồn = 1)")
    private String productName;

    @Schema(description = "Thời gian thực thi đối chứng")
    private LocalDateTime timestamp;

    @Schema(description = "Kết quả của Luồng A (Khách hàng 1)")
    private ThreadResult threadA;

    @Schema(description = "Kết quả của Luồng B (Khách hàng 2)")
    private ThreadResult threadB;

    @Schema(description = "Số lượng tồn kho sau khi kết thúc tranh chấp (phải là 0, không được âm)", example = "0")
    private Integer remainingStock;

    @Schema(description = "Đánh giá tuân thủ ACID Isolation (Serializable/Rowlock/XACT_ABORT)", example = "PASS - Đảm bảo ngăn chặn Overselling và Lost Update tuyệt đối")
    private String acidEvaluation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThreadResult {
        @Schema(description = "Tên khách hàng thử nghiệm")
        private String customerName;

        @Schema(description = "Mã khách hàng")
        private Integer customerId;

        @Schema(description = "Mã trạng thái trả về từ Stored Procedure")
        private Integer statusCode;

        @Schema(description = "Thông báo kết quả")
        private String message;

        @Schema(description = "Mã đơn hàng tạo được (nếu thành công)")
        private Integer orderId;

        @Schema(description = "Số dư ví trước giao dịch")
        private BigDecimal balanceBefore;

        @Schema(description = "Số dư ví sau giao dịch")
        private BigDecimal balanceAfter;

        @Schema(description = "Giao dịch thành công hay bị Rollback")
        private boolean isSuccess;
    }
}
