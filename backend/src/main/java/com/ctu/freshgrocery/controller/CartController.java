package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.AddToCartRequest;
import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.CartResponse;
import com.ctu.freshgrocery.dto.RemoveFromCartRequest;
import com.ctu.freshgrocery.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "2. Giỏ hàng (Cart)", description = "Các API thao tác giỏ hàng thực phẩm thông qua Stored Procedure")
public class CartController {
    private final com.ctu.freshgrocery.customer.CustomerAccess customer;

    private final CartService cartService;

    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng", description = "Thực thi sp_ThemVaoGioHang để kiểm tra tồn kho, hạn sử dụng và cộng dồn số lượng")
    public ResponseEntity<ApiResponse<Void>> addToCart(@Valid @RequestBody AddToCartRequest request) {
        customer.owner(request.getMaKH());
        cartService.addToCart(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .statusCode(1)
                .message("Đã thêm sản phẩm vào giỏ hàng thành công")
                .data(null)
                .build());
    }

    @PatchMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(@Valid @RequestBody AddToCartRequest request) {
        customer.owner(request.getMaKH());
        cartService.updateQuantity(request);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật số lượng", cartService.getCart(request.getMaKH())));
    }

    @DeleteMapping("/items")
    @Operation(summary = "Xóa sản phẩm khỏi giỏ hàng", description = "Thực thi sp_XoaKhoiGioHang để gỡ sản phẩm ra khỏi giỏ")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(@Valid @RequestBody RemoveFromCartRequest request) {
        customer.owner(request.getMaKH());
        cartService.removeFromCart(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .statusCode(1)
                .message("Đã xóa sản phẩm khỏi giỏ hàng thành công")
                .data(null)
                .build());
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Lấy giỏ hàng chi tiết của khách hàng", description = "Thực thi sp_LayGioHangKhachHang để tính toán thành tiền và kiểm tra khả năng đáp ứng tồn kho")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @Parameter(description = "Mã khách hàng", example = "1")
            @PathVariable Integer customerId) {
        customer.owner(customerId);
        CartResponse cartResponse = cartService.getCart(customerId);
        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .success(true)
                .statusCode(1)
                .message("Lấy thông tin giỏ hàng thành công")
                .data(cartResponse)
                .build());
    }
}
