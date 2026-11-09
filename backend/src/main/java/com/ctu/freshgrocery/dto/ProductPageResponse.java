package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả phân trang sản phẩm trả về từ Stored Procedure")
public class ProductPageResponse {
    private List<ProductResponse> items;
    private int currentPage;
    private int pageSize;
    private int totalRecords;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}
