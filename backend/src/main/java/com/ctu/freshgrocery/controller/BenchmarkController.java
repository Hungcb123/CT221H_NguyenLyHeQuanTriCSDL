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
@Tag(name = "8. Đo Kiểm Hiệu Năng Chỉ Mục (Indexing Benchmark 1M Records)", description = "Đối chứng thời gian truy vấn ép quét bảng và ép dùng chỉ mục bao phủ trên các đơn benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    @GetMapping("/compare")
    @Operation(
            summary = "So sánh thời gian quét bảng và dùng chỉ mục bao phủ",
            description = "Thực thi tuần tự hai truy vấn trên cùng các đơn có nguồn BENCHMARK: " +
                    "kịch bản 1 ép quét bảng bằng INDEX(0); kịch bản 2 ép dùng chỉ mục bao phủ. " +
                    "Bảo đảm chỉ mục bao phủ tồn tại trước khi chạy. Trả thời gian đo tại ứng dụng (ms), tỷ số và mức giảm thời gian. " +
                    "Một lượt đo chịu ảnh hưởng của bộ đệm và tải hệ thống; chưa đo I/O hoặc xác nhận Seek/Scan bằng execution plan."
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
            description = "Dùng T-SQL CTE và CROSS JOIN thêm 1.000 đến 1.000.000 đơn có nguồn BENCHMARK vào bảng DonHang. " +
                    "Giá trị count được giới hạn trong khoảng này. Đơn không có chi tiết sản phẩm và được tách khỏi doanh thu bán hàng."
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
