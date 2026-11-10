package com.ctu.freshgrocery.payment;

import com.ctu.freshgrocery.customer.CustomerAccess;
import com.ctu.freshgrocery.dto.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/payments/momo")
public class MoMoController {
    private final CustomerAccess customer;
    private final MoMoService service;
    public record Create(@NotBlank @Size(max=255) String address, @Size(max=255) String note,
                         @NotBlank @Pattern(regexp="[a-zA-Z0-9_-]{16,64}") String idempotencyKey) {}
    @GetMapping("/config") public ApiResponse<?> config(){return ApiResponse.ok("MoMo Sandbox",service.availability());}
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody Create request){
        return ApiResponse.ok("Khởi tạo thanh toán MoMo",service.start(customer.requireId(),request.address().trim(),request.note(),request.idempotencyKey()));
    }
    @GetMapping("/order/{orderId}") public ApiResponse<?> forOrder(@PathVariable int orderId){
        return ApiResponse.ok("Thanh toán của đơn hàng",service.forOrder(orderId,customer.requireId()));
    }
    @GetMapping("/{id}") public ApiResponse<?> status(@PathVariable String id){return ApiResponse.ok("Trạng thái thanh toán",service.status(id,customer.requireId()));}
    @PostMapping("/{id}/refresh") public ApiResponse<?> refresh(@PathVariable String id){return ApiResponse.ok("Đối soát thanh toán",service.refresh(id,customer.requireId()));}
    // Public, server-to-server endpoint. Authentication is the verified MoMo HMAC, never a session or URL resultCode.
    @PostMapping("/ipn") public ResponseEntity<Void> ipn(@RequestBody JsonNode body){service.notification(body);return ResponseEntity.noContent().build();}
}
