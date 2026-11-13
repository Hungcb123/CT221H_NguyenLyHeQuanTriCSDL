package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.TopUpWalletRequest;
import com.ctu.freshgrocery.dto.WalletResponse;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.model.Customer;
import com.ctu.freshgrocery.model.WalletTransaction;
import com.ctu.freshgrocery.repository.CustomerRepository;
import com.ctu.freshgrocery.repository.OrderSpRepository;
import com.ctu.freshgrocery.repository.WalletTransactionRepository;
import com.ctu.freshgrocery.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final CustomerRepository customerRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final OrderSpRepository orderSpRepository;

    @Override
    public WalletResponse topUpWallet(TopUpWalletRequest request) {
        Customer customer = customerRepository.findById(request.getMaKH())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng với ID: " + request.getMaKH()));

        Map<String, Object> result = orderSpRepository.executeNapTienVi(
                request.getMaKH(),
                request.getSoTien(),
                request.getNoiDung(),
                request.getMaYeuCau()
        );

        Integer statusCode = (Integer) result.get("StatusCode");
        String message = (String) result.get("Message");
        BigDecimal soDuMoi = (BigDecimal) result.get("SoDuMoi");

        if (statusCode == null || statusCode < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, message != null ? message : "Lỗi nạp tiền vào ví điện tử");
        }

        return WalletResponse.builder()
                .maKH(customer.getMaKH())
                .hoTen(customer.getHoTen())
                .soDuVi(soDuMoi)
                .build();
    }

    @Override
    public WalletResponse getBalance(Integer customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng với ID: " + customerId));

        return WalletResponse.builder()
                .maKH(customer.getMaKH())
                .hoTen(customer.getHoTen())
                .soDuVi(customer.getSoDuVi())
                .build();
    }

    @Override
    public List<WalletTransaction> getTransactionHistory(Integer customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng với ID: " + customerId);
        }

        return walletTransactionRepository.findCustomerHistory(customerId);
    }
}
