package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.HybridSearchResponse;
import com.ctu.freshgrocery.dto.QdrantStatusResponse;
import com.ctu.freshgrocery.dto.SyncVectorResponse;
import com.ctu.freshgrocery.service.SemanticSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "8. Tìm kiếm Ngữ nghĩa AI (Semantic Vector Search & Qdrant)", description = "Phân hệ tìm kiếm thông minh theo ngữ cảnh/nhu cầu sinh hoạt bằng Vector Embeddings 384 chiều, CSDL Vector Qdrant và mô hình ONNX trên JVM")
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    @GetMapping("/semantic")
    @Operation(
            summary = "Tìm kiếm kết hợp BM25 và vector bằng RRF",
            description = "Tìm kiếm sản phẩm theo ngữ cảnh nấu ăn hoặc nhu cầu sinh hoạt (VD: 'nguyên liệu làm nước ép thanh lọc giảm mỡ', 'thực phẩm giàu protein', 'nguyên liệu nấu canh chua miền Tây'). Hệ thống tự động sinh vector embedding và đối chiếu độ tương đồng Cosine trong CSDL Vector Qdrant."
    )
    public ResponseEntity<ApiResponse<HybridSearchResponse>> searchSemantic(
            @Parameter(description = "Câu truy vấn ngữ nghĩa hoặc nhu cầu người dùng", example = "nguyên liệu làm nước ép thanh lọc giảm mỡ")
            @RequestParam("query") String query,

            @Parameter(description = "Số lượng sản phẩm tối đa trả về", example = "5")
            @RequestParam(value = "limit", defaultValue = "5") int limit,

            @Parameter(description = "Ngưỡng điểm tương đồng Cosine tối thiểu (0.0 đến 1.0)", example = "0.35")
            @RequestParam(value = "minScore", defaultValue = "0.35") Float minScore
    ) {
        HybridSearchResponse results = semanticSearchService.searchWithStatus(query, limit, minScore);
        return ResponseEntity.ok(ApiResponse.<HybridSearchResponse>builder()
                .success(true)
                .statusCode(1)
                .message("Tìm kiếm ngữ nghĩa thành công (Tìm thấy " + results.getProducts().size() + " sản phẩm)")
                .data(results)
                .build());
    }

    @PostMapping("/sync")
    @Operation(
            summary = "Đồng bộ toàn bộ sản phẩm sang CSDL Vector Qdrant (Re-index Vector Collection)",
            description = "Quét toàn bộ sản phẩm thực phẩm tươi sống từ MS SQL Server, chuyển đổi thành vector 384 chiều bằng mô hình ONNX nội bộ và nạp hàng loạt vào collection 'fresh_grocery_products' của Qdrant."
    )
    public ResponseEntity<ApiResponse<SyncVectorResponse>> syncVectors() {
        SyncVectorResponse response = semanticSearchService.syncAllProducts();
        return ResponseEntity.ok(ApiResponse.<SyncVectorResponse>builder()
                .success(response.getTotalSynced() > 0)
                .statusCode(response.getTotalSynced() > 0 ? 1 : -1)
                .message(response.getMessage())
                .data(response)
                .build());
    }

    @GetMapping("/qdrant-status")
    @Operation(
            summary = "Kiểm tra trạng thái CSDL Vector Qdrant & Link Web Dashboard",
            description = "Kiểm tra kết nối tới Qdrant, số lượng points đã đánh chỉ mục, mô hình embedding và cung cấp đường dẫn mở trực tiếp Web UI Dashboard (http://localhost:6333/dashboard) phục vụ báo cáo với Thầy/Cô."
    )
    public ResponseEntity<ApiResponse<QdrantStatusResponse>> getQdrantStatus() {
        QdrantStatusResponse status = semanticSearchService.getStatus();
        return ResponseEntity.ok(ApiResponse.<QdrantStatusResponse>builder()
                .success("ONLINE".equals(status.getStatus()))
                .statusCode("ONLINE".equals(status.getStatus()) ? 1 : 0)
                .message("Lấy thông tin trạng thái Qdrant thành công")
                .data(status)
                .build());
    }
}
