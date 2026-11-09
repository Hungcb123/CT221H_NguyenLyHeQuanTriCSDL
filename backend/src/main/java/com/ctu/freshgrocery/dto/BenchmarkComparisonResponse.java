package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả so sánh hiệu năng chỉ mục (Index Optimization Benchmark 1M dòng)")
public class BenchmarkComparisonResponse {

    @Schema(description = "Tổng số lượng dòng dữ liệu đơn hàng hiện có trong bảng DonHang", example = "500000")
    private Long totalOrdersInDatabase;

    private Long benchmarkOrdersInDatabase;

    @Schema(description = "Kịch bản 1: Không sử dụng Index (Clustered Index Scan / Table Scan)")
    private ScenarioResult withoutIndex;

    @Schema(description = "Kịch bản 2: Sử dụng Covering Index Seek (IX_DonHang_NgayTao_TrangThai_Covering)")
    private ScenarioResult withCoveringIndex;

    @Schema(description = "Tốc độ cải thiện (Số lần nhanh hơn)", example = "16.4x")
    private String speedupFactor;

    @Schema(description = "Tỷ lệ phần trăm thời gian thực thi được cắt giảm", example = "94.2%")
    private String timeReductionPercentage;

    @Schema(description = "Đánh giá học thuật môn CSDL CT221H", example = "PASS - Covering Index loại bỏ hoàn toàn việc quét bảng tuần tự, chuyển sang tìm kiếm nhị phân B-Tree Seek.")
    private String academicEvaluation;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioResult {
        @Schema(description = "Tên kịch bản đo kiểm")
        private String scenarioName;

        @Schema(description = "Phương thức truy xuất dữ liệu (Scan vs Seek)")
        private String accessMethod;

        @Schema(description = "Thời gian thực thi (mili-giây)")
        private Long executionTimeMs;

        @Schema(description = "Số dòng kết quả trả về")
        private Integer recordsReturned;

        @Schema(description = "Ghi chú chiến lược thực thi trong Execution Plan")
        private String executionPlanNote;
    }
}
