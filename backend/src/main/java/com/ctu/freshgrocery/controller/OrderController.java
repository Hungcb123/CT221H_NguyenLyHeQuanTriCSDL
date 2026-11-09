package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.CheckoutRequest;
import com.ctu.freshgrocery.dto.CheckoutResponse;
import com.ctu.freshgrocery.dto.ConcurrencyTestResponse;
import com.ctu.freshgrocery.dto.OrderDetailResponse;
import com.ctu.freshgrocery.service.OrderService;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "4. Đơn hàng & Thanh toán ACID (Orders & Concurrency Demo)", description = "Các API thanh toán đơn hàng bằng T-SQL Stored Procedure và Demo đối chứng tranh chấp đồng thời")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @Operation(
            summary = "Thanh toán đơn hàng qua ví điện tử (REQ-TX-01)",
            description = "Thực thi thủ tục sp_ThanhToanDonHang chuẩn ACID: khóa UPDLOCK/ROWLOCK kiểm tra tồn kho, trừ tiền ví, tạo đơn hàng, ghi sổ cái kiểm toán và xóa giỏ hàng nguyên tử."
    )
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(@Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = orderService.checkout(request);
        return ResponseEntity.ok(ApiResponse.<CheckoutResponse>builder()
                .success(true)
                .statusCode(response.getStatusCode())
                .message(response.getMessage())
                .data(response)
                .build());
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Xem chi tiết hóa đơn đơn hàng", description = "Lấy đầy đủ thông tin hóa đơn, danh sách mặt hàng, số lượng và giá tại thời điểm mua")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderById(
            @Parameter(description = "Mã hóa đơn đơn hàng", example = "1")
            @PathVariable Integer orderId) {
        OrderDetailResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.<OrderDetailResponse>builder()
                .success(true)
                .statusCode(1)
                .message("Lấy thông tin đơn hàng thành công")
                .data(response)
                .build());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Xem lịch sử đơn hàng của khách hàng", description = "Truy vấn danh sách đơn hàng đã đặt theo thứ tự thời gian mới nhất")
    public ResponseEntity<ApiResponse<List<OrderDetailResponse>>> getOrdersByCustomer(
            @Parameter(description = "Mã khách hàng", example = "1")
            @PathVariable Integer customerId) {
        List<OrderDetailResponse> response = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.<List<OrderDetailResponse>>builder()
                .success(true)
                .statusCode(1)
                .message("Lấy danh sách đơn hàng của khách thành công")
                .data(response)
                .build());
    }

    @PostMapping("/concurrency-test")
    @Operation(
            summary = "1-Click Concurrency Demo: Kiểm thử tranh chấp đồng thời 2 luồng",
            description = "Tự động thiết lập sản phẩm Flash Sale có tồn kho = 1. Kích hoạt đồng thời 2 luồng độc lập của 2 khách hàng khác nhau cùng bấm thanh toán tại cùng mili-giây. " +
                    "Hệ thống chứng minh tính đúng đắn của T-SQL Transaction (XACT_ABORT ON, UPDLOCK): 1 giao dịch Thành công (trừ kho về 0, trừ tiền ví) và 1 giao dịch Thất bại/Rollback (báo hết hàng, ví tiền giữ nguyên tuyệt đối)."
    )
    public ResponseEntity<ApiResponse<ConcurrencyTestResponse>> runConcurrencyTest() {
        ConcurrencyTestResponse response = orderService.runConcurrencyTest();
        return ResponseEntity.ok(ApiResponse.<ConcurrencyTestResponse>builder()
                .success(true)
                .statusCode(1)
                .message("Thực hiện kiểm thử tranh chấp đồng thời hoàn tất!")
                .data(response)
                .build());
    }
}
