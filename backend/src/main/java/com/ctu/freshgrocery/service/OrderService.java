package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.CheckoutRequest;
import com.ctu.freshgrocery.dto.CheckoutResponse;
import com.ctu.freshgrocery.dto.ConcurrencyTestResponse;
import com.ctu.freshgrocery.dto.OrderDetailResponse;

import java.util.List;

public interface OrderService {
    CheckoutResponse checkout(CheckoutRequest request);
    OrderDetailResponse getOrderById(Integer orderId);
    List<OrderDetailResponse> getOrdersByCustomer(Integer customerId);
    List<OrderDetailResponse> getOrdersByCustomer(Integer customerId, int page, int size);
    ConcurrencyTestResponse runConcurrencyTest();
}
