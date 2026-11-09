package com.ctu.freshgrocery.config;

import com.ctu.freshgrocery.service.QdrantService;
import com.ctu.freshgrocery.service.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticSearchInitializer {

    private final QdrantService qdrantService;
    private final SemanticSearchService semanticSearchService;

    @Value("${qdrant.collection-name:fresh_grocery_products}")
    private String collectionName;

    @Value("${qdrant.dimension:384}")
    private int dimension;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        new Thread(() -> {
            try {
                // Đợi 2 giây cho các container và CSDL ổn định
                Thread.sleep(2000);
                log.info("Kiểm tra trạng thái CSDL Vector Qdrant khi khởi động hệ thống...");

                if (qdrantService.isHealthy()) {
                    log.info("✅ Kết nối thành công đến CSDL Vector Qdrant!");
                    qdrantService.createCollectionIfNotExists(collectionName, dimension);

                    Long count = qdrantService.getPointsCount(collectionName);
                    log.info("Số lượng vector điểm hiện có trong collection [{}]: {}", collectionName, count);

                    if (count == null || count == 0L) {
                        log.info("Collection trống, tự động nạp toàn bộ vector sản phẩm từ MS SQL Server sang Qdrant...");
                        semanticSearchService.syncAllProducts();
                    }
                } else {
                    log.warn("⚠️ Qdrant Vector DB hiện chưa online hoặc chưa sẵn sàng. Bạn có thể kích hoạt sync thủ công sau qua POST /api/v1/search/sync khi bật container Qdrant.");
                }
            } catch (Exception e) {
                log.warn("Lỗi khi tự động khởi tạo Qdrant: {}", e.getMessage());
            }
        }, "qdrant-init-thread").start();
    }
}
