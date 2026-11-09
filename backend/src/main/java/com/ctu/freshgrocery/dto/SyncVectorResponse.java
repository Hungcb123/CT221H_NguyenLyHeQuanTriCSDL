package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả đồng bộ dữ liệu vector từ MS SQL Server sang CSDL Vector Qdrant")
public class SyncVectorResponse {

    @Schema(description = "Tổng số sản phẩm đã quét trong MS SQL Server", example = "12")
    private int totalProcessed;

    @Schema(description = "Số lượng vector đã nạp thành công vào Qdrant", example = "12")
    private int totalSynced;

    @Schema(description = "Tên collection trong Qdrant", example = "fresh_grocery_products")
    private String collectionName;

    @Schema(description = "Thời gian thực thi đồng bộ (mili-giây)", example = "125")
    private long durationMs;

    @Schema(description = "Thông báo kết quả", example = "Đồng bộ thành công 12 sản phẩm sang CSDL Vector Qdrant!")
    private String message;
}
