package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.AlertLogResponse;
import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.service.AlertLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "2. Kiểm toán & Nhật ký Cảnh báo Trigger (Audit Alerts Module)", description = "Đối soát các cảnh báo do Trigger SQL Server tự động bẫy sự kiện ghi vào bảng NhatKyCanhBao")
public class AlertLogController {

    private final AlertLogService alertLogService;

    @GetMapping
    @Operation(
            summary = "Xem 20 cảnh báo mới nhất từ Trigger SQL Server",
            description = """
                    Đọc dữ liệu từ bảng **NhatKyCanhBao**.
                    Dùng để chứng minh và kiểm tra các Trigger sau khi thực thi các thao tác cập nhật giá, hết hàng hoặc xóa mềm:
                    - `CANH_BAO_HET_HANG` (Trigger AFTER UPDATE)
                    - `CANH_BAO_GIA_BAT_THUONG` (Trigger AFTER UPDATE)
                    - `CANH_BAO_CAN_DATE` (Trigger AFTER UPDATE)
                    - `XOA_MEM_SAN_PHAM` (Trigger INSTEAD OF DELETE)
                    """
    )
    public ResponseEntity<ApiResponse<List<AlertLogResponse>>> getRecentAlerts() {
        return ResponseEntity.ok(alertLogService.getRecentAlerts());
    }

    @GetMapping("/product/{productId}")
    @Operation(
            summary = "Xem lịch sử cảnh báo theo mã sản phẩm cụ thể",
            description = "Tra cứu toàn bộ các sự kiện bẫy liên quan đến một mã sản phẩm cụ thể."
    )
    public ResponseEntity<ApiResponse<List<AlertLogResponse>>> getAlertsByProduct(
            @Parameter(description = "Mã sản phẩm cần tra cứu", example = "1")
            @PathVariable Integer productId) {
        return ResponseEntity.ok(alertLogService.getAlertsByProduct(productId));
    }

    @GetMapping("/filter")
    @Operation(
            summary = "Lọc cảnh báo theo loại sự kiện",
            description = "Lọc theo: CANH_BAO_HET_HANG, CANH_BAO_GIA_BAT_THUONG, CANH_BAO_CAN_DATE, XOA_MEM_SAN_PHAM"
    )
    public ResponseEntity<ApiResponse<List<AlertLogResponse>>> getAlertsByEventType(
            @Parameter(description = "Loại sự kiện cần lọc", example = "CANH_BAO_GIA_BAT_THUONG")
            @RequestParam String eventType) {
        return ResponseEntity.ok(alertLogService.getAlertsByEventType(eventType));
    }
}
