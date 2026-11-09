package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.ProductCreateRequest;
import com.ctu.freshgrocery.dto.ProductPageResponse;
import com.ctu.freshgrocery.dto.ProductResponse;
import com.ctu.freshgrocery.dto.ProductUpdateRequest;

import java.math.BigDecimal;

public interface ProductService {

    ApiResponse<ProductResponse> createProduct(ProductCreateRequest request);

    ApiResponse<ProductResponse> updateProduct(Integer id, ProductUpdateRequest request);

    ApiResponse<String> deleteProduct(Integer id);

    ApiResponse<ProductPageResponse> getProducts(
            Integer categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            int page,
            int size
    );

    ApiResponse<ProductResponse> getProductById(Integer id);
}
