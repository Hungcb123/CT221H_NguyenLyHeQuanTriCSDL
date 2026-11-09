package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.AutoClearanceResponse;
import com.ctu.freshgrocery.dto.PromotionAddProductRequest;
import com.ctu.freshgrocery.dto.PromotionCreateRequest;
import com.ctu.freshgrocery.dto.PromotionDetailResponse;
import com.ctu.freshgrocery.dto.PromotionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionService {

    List<PromotionResponse> getAllPromotions(Boolean activeOnly);

    PromotionResponse getPromotionById(Integer id);

    ApiResponse<PromotionResponse> createPromotion(PromotionCreateRequest request);

    ApiResponse<PromotionDetailResponse> addProductToPromotion(Integer promotionId, PromotionAddProductRequest request);

    AutoClearanceResponse triggerAutoClearance(Integer soNgayCanDate, BigDecimal phanTramGiam);
}
