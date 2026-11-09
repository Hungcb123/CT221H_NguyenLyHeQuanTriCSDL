package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.TopUpWalletRequest;
import com.ctu.freshgrocery.dto.WalletResponse;
import com.ctu.freshgrocery.model.WalletTransaction;
import com.ctu.freshgrocery.service.WalletService;
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
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "3. Ví điện tử & Sổ cái kiểm toán (Wallet & Audit Trail)", description = "Các API nạp tiền và theo dõi dòng tiền bảo toàn chuẩn ACID")
public class WalletController {
    private final com.ctu.freshgrocery.customer.CustomerAccess customer;

    private final WalletService walletService;

    @PostMapping("/top-up")
    @Operation(summary = "Nạp tiền vào ví điện tử", description = "Thực thi sp_NapTienVi có khóa UPDLOCK ngăn Lost Update và ghi sổ cái kiểm toán LichSuGiaoDichVi")
    public ResponseEntity<ApiResponse<WalletResponse>> topUpWallet(@Valid @RequestBody TopUpWalletRequest request) {
        customer.owner(request.getMaKH());
        WalletResponse response = walletService.topUpWallet(request);
        return ResponseEntity.ok(ApiResponse.<WalletResponse>builder()
                .success(true)
                .statusCode(1)
                .message("Nạp tiền vào ví thành công")
                .data(response)
                .build());
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Xem số dư ví điện tử của khách hàng", description = "Truy vấn thông tin tài khoản và số dư ví khả dụng")
    public ResponseEntity<ApiResponse<WalletResponse>> getBalance(
            @Parameter(description = "Mã khách hàng", example = "1")
            @PathVariable Integer customerId) {
        customer.owner(customerId);
        WalletResponse response = walletService.getBalance(customerId);
        return ResponseEntity.ok(ApiResponse.<WalletResponse>builder()
                .success(true)
                .statusCode(1)
                .message("Lấy số dư ví thành công")
                .data(response)
                .build());
    }

    @GetMapping("/{customerId}/transactions")
    @Operation(summary = "Xem sổ cái lịch sử giao dịch ví", description = "Truy vấn toàn bộ audit trail biến động dòng tiền (Nạp tiền, Thanh toán, Hoàn tiền) của khách hàng")
    public ResponseEntity<ApiResponse<List<WalletTransaction>>> getTransactions(
            @Parameter(description = "Mã khách hàng", example = "1")
            @PathVariable Integer customerId) {
        customer.owner(customerId);
        List<WalletTransaction> transactions = walletService.getTransactionHistory(customerId);
        return ResponseEntity.ok(ApiResponse.<List<WalletTransaction>>builder()
                .success(true)
                .statusCode(1)
                .message("Lấy lịch sử giao dịch thành công")
                .data(transactions)
                .build());
    }
}
