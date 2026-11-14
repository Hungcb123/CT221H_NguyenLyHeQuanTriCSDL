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
@RequestMapping("/api/v1/payments/sepay")
public class SePayController {
    private final CustomerAccess customer;
    private final SePayService service;
    public record Create(@NotBlank @Size(max=255) String address, @Size(max=255) String note,
                         @NotBlank @Pattern(regexp="[a-zA-Z0-9_-]{16,64}") String idempotencyKey) {}
    @GetMapping("/config") public ApiResponse<?> config(){return ApiResponse.ok("SePay",service.availability());}
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody Create request){
        return ApiResponse.ok("Khởi tạo thanh toán SePay",service.start(customer.requireId(),request.address().trim(),request.note(),request.idempotencyKey()));
    }
    @GetMapping("/order/{orderId}") public ApiResponse<?> forOrder(@PathVariable int orderId){
        return ApiResponse.ok("Thanh toán của đơn hàng",service.forOrder(orderId,customer.requireId()));
    }
    @GetMapping("/{id}") public ApiResponse<?> status(@PathVariable String id){return ApiResponse.ok("Trạng thái thanh toán",service.status(id,customer.requireId()));}
    @PostMapping("/{id}/refresh") public ApiResponse<?> refresh(@PathVariable String id){return ApiResponse.ok("Đối soát thanh toán",service.status(id,customer.requireId()));}
    @PostMapping("/webhook") public ResponseEntity<?> webhook(
            @RequestHeader(value="Authorization",required=false) String authorization, @RequestBody JsonNode body) {
        service.notification(authorization,body);
        return ResponseEntity.status(201).body(java.util.Map.of("success",true));
    }
}
