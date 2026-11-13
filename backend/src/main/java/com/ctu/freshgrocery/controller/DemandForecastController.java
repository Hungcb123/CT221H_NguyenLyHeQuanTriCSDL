package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.BatchForecastResponse;
import com.ctu.freshgrocery.dto.ForecastCustomRequest;
import com.ctu.freshgrocery.dto.ForecastResponse;
import com.ctu.freshgrocery.service.DemandForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/forecast")
@RequiredArgsConstructor
@Tag(name = "5. AI Dự báo Nhu cầu & Nhập kho (Machine Learning ONNX)", description = "Các API suy luận Machine Learning trực tiếp trên Java JVM (<5ms) và đề xuất nhập hàng tươi sống")
public class DemandForecastController {

    private final DemandForecastService demandForecastService;

    @GetMapping("/product/{productId}")
    @Operation(
            summary = "Dự báo tự động nhu cầu tiêu thụ ngày mai theo Mã sản phẩm",
            description = "Hệ thống tự động trích xuất các thuộc tính thời gian (Thứ trong tuần, cuối tuần, tháng), giá bán và ước tính chỉ số Lag từ CSDL; " +
                    "chạy suy luận mô hình PyTorch-MLP-ONNX qua ONNX Runtime Java, đề xuất lượng nhập hàng tươi sống (buffer 10%) và lưu vào bảng DuBaoNhapHang."
    )
    public ResponseEntity<ApiResponse<ForecastResponse>> predictForProduct(
            @Parameter(description = "Mã sản phẩm tươi sống", example = "1")
            @PathVariable Integer productId) {
        ForecastResponse response = demandForecastService.predictForProduct(productId);
        return ResponseEntity.ok(ApiResponse.<ForecastResponse>builder()
                .success(true)
                .statusCode(1)
                .message("Dự báo nhu cầu AI hoàn tất với độ trễ " + response.getDoTreInferenceMs() + "ms")
                .data(response)
                .build());
    }

    @PostMapping("/predict-custom")
    @Operation(
            summary = "Dự báo nhu cầu với kịch bản giả lập tùy chỉnh trên Swagger UI",
            description = "Thử tham số giả lập và xem kết quả trực tiếp, không ghi lịch sử hay cảnh báo vận hành."
    )
    public ResponseEntity<ApiResponse<ForecastResponse>> predictCustom(@Valid @RequestBody ForecastCustomRequest request) {
        ForecastResponse response = demandForecastService.predictCustom(request);
        return ResponseEntity.ok(ApiResponse.<ForecastResponse>builder()
                .success(true)
                .statusCode(1)
                .message("Chạy suy luận kịch bản giả lập AI hoàn tất trong " + response.getDoTreInferenceMs() + "ms")
                .data(response)
                .build());
    }

    @PostMapping("/run-batch")
    @Operation(
            summary = "Chạy dự báo hàng loạt toàn bộ danh mục thực phẩm tươi sống",
            description = "Quét qua toàn bộ các mặt hàng đang kinh doanh, thực thi suy luận liên tục trên Java JVM và đồng bộ bảng DuBaoNhapHang, cảnh báo các món có nguy cơ ế hàng quá date."
    )
    public ResponseEntity<ApiResponse<BatchForecastResponse>> runBatchForecast() {
        BatchForecastResponse response = demandForecastService.runBatchForecast();
        return ResponseEntity.ok(ApiResponse.<BatchForecastResponse>builder()
                .success(true)
                .statusCode(1)
                .message("Đã hoàn tất dự báo hàng loạt cho " + response.getTongSoMatHang() + " mặt hàng tươi sống")
                .data(response)
                .build());
    }

    @GetMapping("/history/{productId}")
    @Operation(summary = "Xem lịch sử dự báo của một sản phẩm trong CSDL", description = "Truy vấn danh sách các kết quả dự báo đã được lưu trữ trong bảng DuBaoNhapHang")
    public ResponseEntity<ApiResponse<List<ForecastResponse>>> getForecastHistory(
            @Parameter(description = "Mã sản phẩm", example = "1")
            @PathVariable Integer productId) {
        List<ForecastResponse> history = demandForecastService.getForecastHistory(productId);
        return ResponseEntity.ok(ApiResponse.<List<ForecastResponse>>builder()
                .success(true)
                .statusCode(1)
                .message("Lấy lịch sử dự báo thành công")
                .data(history)
                .build());
    }
}
