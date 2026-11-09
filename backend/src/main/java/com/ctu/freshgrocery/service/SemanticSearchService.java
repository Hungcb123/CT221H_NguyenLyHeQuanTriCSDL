package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.HybridSearchResponse;
import com.ctu.freshgrocery.dto.QdrantStatusResponse;
import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.ctu.freshgrocery.dto.SyncVectorResponse;
import com.ctu.freshgrocery.model.Product;

import java.util.List;

public interface SemanticSearchService {

    /**
     * Tìm kiếm sản phẩm kết hợp (Hybrid Search: Vector Qdrant + Từ khóa SQL)
     * @param query Câu truy vấn ngữ nghĩa hoặc nhu cầu nấu ăn của khách hàng
     * @param limit Số lượng kết quả tối đa
     * @param minScore Ngưỡng điểm tương đồng tối thiểu (0.0 đến 1.0)
     * @return Danh sách sản phẩm kèm điểm tương đồng Cosine
     */
    List<SemanticProductResponse> search(String query, int limit, Float minScore);

    HybridSearchResponse searchWithStatus(String query, int limit, Float minScore);

    /**
     * Đồng bộ toàn bộ sản phẩm từ MS SQL Server sang CSDL Vector Qdrant
     */
    SyncVectorResponse syncAllProducts();

    /**
     * Đồng bộ một sản phẩm khi được tạo mới hoặc cập nhật
     */
    void syncProduct(Product product);

    /**
     * Xóa vector của sản phẩm khỏi Qdrant khi sản phẩm bị xóa mềm
     */
    void removeProduct(Integer productId);

    /**
     * Lấy thông tin trạng thái kết nối Qdrant và link Web UI Dashboard
     */
    QdrantStatusResponse getStatus();
}
