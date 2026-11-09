package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.AddToCartRequest;
import com.ctu.freshgrocery.dto.CartItemResponse;
import com.ctu.freshgrocery.dto.CartResponse;
import com.ctu.freshgrocery.dto.RemoveFromCartRequest;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.model.Cart;
import com.ctu.freshgrocery.repository.CartRepository;
import com.ctu.freshgrocery.repository.CustomerRepository;
import com.ctu.freshgrocery.repository.OrderSpRepository;
import com.ctu.freshgrocery.service.CartService;
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
public class CartServiceImpl implements CartService {

    private final OrderSpRepository orderSpRepository;
    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;

    @Override
    public void addToCart(AddToCartRequest request) {
        if (!customerRepository.existsById(request.getMaKH())) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng với ID: " + request.getMaKH());
        }

        Map<String, Object> result = orderSpRepository.executeThemVaoGioHang(
                request.getMaKH(),
                request.getMaSP(),
                request.getSoLuong()
        );

        Integer statusCode = (Integer) result.get("StatusCode");
        String message = (String) result.get("Message");

        if (statusCode == null || statusCode < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, message != null ? message : "Lỗi thêm sản phẩm vào giỏ hàng");
        }
    }

    @Override
    public void removeFromCart(RemoveFromCartRequest request) {
        Map<String, Object> result = orderSpRepository.executeXoaKhoiGioHang(
                request.getMaKH(),
                request.getMaSP()
        );

        Integer statusCode = (Integer) result.get("StatusCode");
        String message = (String) result.get("Message");

        if (statusCode == null || statusCode < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, message != null ? message : "Lỗi xóa sản phẩm khỏi giỏ hàng");
        }
    }

    @Override
    public CartResponse getCart(Integer customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng với ID: " + customerId);
        }

        List<CartItemResponse> items = orderSpRepository.executeLayGioHangKhachHang(customerId);

        BigDecimal tongTien = items.stream()
                .map(CartItemResponse::getThanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tongTienTietKiem = items.stream()
                .map(item -> item.getTienTietKiem() != null ? item.getTienTietKiem() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int tongSoLuong = items.stream()
                .mapToInt(CartItemResponse::getSoLuong)
                .sum();

        Integer maGioHang = cartRepository.findByMaKH(customerId)
                .map(Cart::getMaGioHang)
                .orElse(0);

        return CartResponse.builder()
                .maGioHang(maGioHang)
                .maKH(customerId)
                .tongTien(tongTien)
                .tongSoLuong(tongSoLuong)
                .tongTienTietKiem(tongTienTietKiem)
                .items(items)
                .build();
    }
}
