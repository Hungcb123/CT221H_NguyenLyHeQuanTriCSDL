package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.BenchmarkComparisonResponse;
import com.ctu.freshgrocery.service.BenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/benchmark")
@RequiredArgsConstructor
@Tag(name = "8. Đo Kiểm Hiệu Năng Chỉ Mục (Indexing Benchmark 1M Records)", description = "Các API đối chứng hiệu năng truy vấn dữ liệu lớn: Table Scan vs B-Tree Covering Index Seek")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    @GetMapping("/compare")
    @Operation(
            summary = "1-Click Benchmark: So sánh trực tiếp Table Scan vs Covering Index Seek",
            description = "Thực thi song song 2 kịch bản truy vấn phân tích doanh số trên bảng DonHang lớn: " +
                    "Kịch bản 1 (Clustered Index Scan - Không dùng chỉ mục bao phủ) vs Kịch bản 2 (Covering Index Seek - Đã tối ưu B-Tree + INCLUDE). " +
                    "Trả về kết quả thời gian thực thi (ms), hệ số tăng tốc (Speedup Factor) và tỷ lệ thời gian I/O cắt giảm."
    )
    public ResponseEntity<ApiResponse<BenchmarkComparisonResponse>> compareBenchmark() {
        BenchmarkComparisonResponse response = benchmarkService.compareBenchmark();
        return ResponseEntity.ok(ApiResponse.<BenchmarkComparisonResponse>builder()
                .success(true)
                .statusCode(1)
                .message("Đo kiểm hiệu năng hoàn tất! Tốc độ cải thiện: " + response.getSpeedupFactor())
                .data(response)
                .build());
    }

    @PostMapping("/generate-data")
    @Operation(
            summary = "Kích hoạt sinh dữ liệu lớn mẫu vào bảng DonHang bằng T-SQL CTE",
            description = "Sử dụng đệ quy T-SQL Common Table Expression (CTE) kết hợp CROSS JOIN sinh cực nhanh 10.000 đến 500.000 bản ghi đơn hàng mẫu."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateData(
            @Parameter(description = "Số lượng đơn hàng cần sinh (mặc định: 50.000 dòng)", example = "50000")
            @RequestParam(required = false, defaultValue = "50000") Integer count) {
        Map<String, Object> result = benchmarkService.generateData(count);
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .statusCode(1)
                .message("Sinh dữ liệu lớn thành công!")
                .data(result)
                .build());
    }

    @PostMapping("/indexes/toggle")
    @Operation(
            summary = "Bật / Tắt chỉ mục bao phủ (Covering Index) trên bảng DonHang",
            description = "Cho phép tạo mới hoặc gỡ bỏ IX_DonHang_NgayTao_TrangThai_Covering để Quản trị viên và Thầy/Cô có thể quan sát sự khác biệt khi chạy truy vấn trực tiếp."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleIndex(
            @Parameter(description = "Bật (true) hoặc Tắt (false) Covering Index", example = "true")
            @RequestParam boolean enable) {
        Map<String, Object> result = benchmarkService.toggleCoveringIndex(enable);
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .statusCode(1)
                .message("Thay đổi trạng thái chỉ mục thành công")
                .data(result)
                .build());
    }
}
