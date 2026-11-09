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
@Schema(description = "Thông tin trạng thái kết nối và chỉ mục của CSDL Vector Qdrant")
public class QdrantStatusResponse {

    @Schema(description = "Trạng thái hoạt động", example = "ONLINE")
    private String status;

    @Schema(description = "Tên collection sản phẩm", example = "fresh_grocery_products")
    private String collectionName;

    @Schema(description = "Số lượng vector điểm (points) đang lưu trữ trong Qdrant", example = "12")
    private Long pointsCount;

    @Schema(description = "Số chiều của vector embedding", example = "384")
    private Integer vectorDimension;

    @Schema(description = "Mô hình Embedding đang sử dụng", example = "all-MiniLM-L6-v2 (ONNX Runtime JVM)")
    private String modelName;

    @Schema(description = "Khoảng cách đo lường độ tương đồng", example = "Cosine")
    private String distanceMetric;

    @Schema(description = "Đường dẫn trực tiếp đến Web UI Dashboard của Qdrant", example = "http://localhost:6333/dashboard")
    private String qdrantDashboardUrl;
}
