package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.AddToCartRequest;
import com.ctu.freshgrocery.dto.CartResponse;
import com.ctu.freshgrocery.dto.RemoveFromCartRequest;

public interface CartService {
    void addToCart(AddToCartRequest request);
    void updateQuantity(AddToCartRequest request);
    void removeFromCart(RemoveFromCartRequest request);
    CartResponse getCart(Integer customerId);
}
