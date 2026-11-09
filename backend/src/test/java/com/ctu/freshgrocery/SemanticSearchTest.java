package com.ctu.freshgrocery;

import com.ctu.freshgrocery.dto.QdrantStatusResponse;
import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.ctu.freshgrocery.dto.SyncVectorResponse;
import com.ctu.freshgrocery.model.Category;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.repository.CategoryRepository;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.service.EmbeddingService;
import com.ctu.freshgrocery.service.QdrantService;
import com.ctu.freshgrocery.service.impl.EmbeddingServiceImpl;
import com.ctu.freshgrocery.service.impl.SemanticSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import com.ctu.freshgrocery.exception.AppException;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SemanticSearchTest {

    private EmbeddingServiceImpl embeddingService;
    private QdrantService qdrantService;
    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private SemanticSearchServiceImpl semanticSearchService;

    @BeforeEach
    public void setup() {
        embeddingService = new EmbeddingServiceImpl();
        embeddingService.init();

        qdrantService = Mockito.mock(QdrantService.class);
        productRepository = Mockito.mock(ProductRepository.class);
        categoryRepository = Mockito.mock(CategoryRepository.class);
        com.ctu.freshgrocery.repository.PromotionDetailRepository promoDetailRepo = Mockito.mock(com.ctu.freshgrocery.repository.PromotionDetailRepository.class);

        semanticSearchService = new SemanticSearchServiceImpl(
                embeddingService,
                qdrantService,
                productRepository,
                categoryRepository,
                promoDetailRepo
        );

        ReflectionTestUtils.setField(semanticSearchService, "collectionName", "fresh_grocery_products");
        ReflectionTestUtils.setField(semanticSearchService, "dimension", 384);
    }

    @AfterEach
    void cleanup() {
        embeddingService.cleanup();
    }

    @Test
    void unavailableModelDoesNotGenerateFakeEmbedding() {
        EmbeddingServiceImpl unavailable = new EmbeddingServiceImpl();
        AppException error = assertThrows(AppException.class,
                () -> unavailable.generateEmbedding("rau xanh"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
    }

    @Test
    void qdrantFailureReturnsAnExplicitKeywordFallback() {
        when(qdrantService.search(anyString(), any(), anyInt(), anyFloat()))
                .thenThrow(new IllegalStateException("offline"));
        var result = semanticSearchService.searchWithStatus("rau xanh", 12, 0.3f);
        assertEquals("KEYWORD_FALLBACK", result.getMode());
        assertTrue(result.getProducts().isEmpty());
        assertEquals(0f, result.getSemanticWeight());
        assertEquals(1f, result.getKeywordWeight());
    }

    @Test
    public void testEmbeddingGenerationAndCosineProperties() {
        float[] vecCelery = embeddingService.generateEmbedding("cần tây sạch nước ép thanh lọc detox");
        assertNotNull(vecCelery);
        assertEquals(384, vecCelery.length, "Vector embedding phải có đúng 384 chiều");

        // Kiểm tra độ dài chuẩn hóa L2 norm xấp xỉ 1.0
        float sumSq = 0f;
        for (float v : vecCelery) sumSq += v * v;
        assertEquals(1.0f, (float) Math.sqrt(sumSq), 0.01f, "Vector phải được L2 normalized");

        float[] vecQuery = embeddingService.generateEmbedding("nước ép thanh lọc cơ thể giảm cân");
        float[] vecPork = embeddingService.generateEmbedding("thịt ba chỉ heo tươi sạch kho tàu");

        // Tính Cosine Similarity (Dot product do đã L2 normalize)
        float dotCelery = 0f;
        float dotPork = 0f;
        for (int i = 0; i < 384; i++) {
            dotCelery += vecQuery[i] * vecCelery[i];
            dotPork += vecQuery[i] * vecPork[i];
        }

        assertTrue(dotCelery > dotPork, "Độ tương đồng của Cần tây với nước ép thanh lọc (" + dotCelery 
                + ") phải cao hơn Thịt ba chỉ (" + dotPork + ")");
    }

    @Test
    public void testHybridSemanticSearchWithQdrantResults() {
        Category catVegetable = Category.builder().maDanhMuc(1).tenDanhMuc("Rau củ quả tươi").build();
        Product pCelery = Product.builder()
                .maSP(10)
                .tenSP("Cần tây sạch Đà Lạt")
                .moTa("Cần tây tươi ngon ép lấy nước thanh lọc cơ thể")
                .gia(new BigDecimal("35000.00"))
                .donViTinh("kg")
                .soLuongTon(20)
                .hanSuDung(LocalDateTime.now().plusDays(4))
                .maDanhMuc(1)
                .trangThaiXoa(false)
                .build();

        when(categoryRepository.findAll()).thenReturn(List.of(catVegetable));
        when(productRepository.findAllById(List.of(10))).thenReturn(List.of(pCelery));
        when(productRepository.findAll()).thenReturn(List.of(pCelery));

        QdrantService.QdrantScoredPoint point = QdrantService.QdrantScoredPoint.builder()
                .id(10L)
                .score(0.85f)
                .payload(Map.of("tenSP", "Cần tây sạch Đà Lạt"))
                .build();

        when(qdrantService.search(eq("fresh_grocery_products"), any(), anyInt(), anyFloat()))
                .thenReturn(List.of(point));

        List<SemanticProductResponse> results = semanticSearchService.search("nước ép thanh lọc", 5, 0.4f);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        SemanticProductResponse top = results.get(0);
        assertEquals(10, top.getMaSP());
        assertEquals("Cần tây sạch Đà Lạt", top.getTenSP());
        assertEquals(0.85f, top.getSimilarityScore());
        assertEquals("Rau củ quả tươi", top.getTenDanhMuc());
        assertEquals(pCelery.getSoLuongTon(), top.getSoLuongTon());
        assertEquals(pCelery.getHanSuDung(), top.getHanSuDung());
        assertEquals(pCelery.getGia(), top.getGiaHienTai());
        assertEquals("HYBRID", top.getMatchType());
        assertEquals(1f / 61, top.getRankingScore(), 0.0001f);
    }

    @Test
    public void testSyncAllProducts() {
        Category cat = Category.builder().maDanhMuc(3).tenDanhMuc("Thủy hải sản tươi").build();
        Product p = Product.builder()
                .maSP(1)
                .tenSP("Cá lóc tươi làm sạch")
                .moTa("Cá lóc đồng làm sạch nấu canh chua")
                .gia(new BigDecimal("75000.00"))
                .donViTinh("con")
                .soLuongTon(15)
                .hanSuDung(LocalDateTime.now().plusDays(2))
                .maDanhMuc(3)
                .trangThaiXoa(false)
                .build();

        when(categoryRepository.findAll()).thenReturn(List.of(cat));
        when(productRepository.findAll()).thenReturn(List.of(p));
        when(qdrantService.createCollectionIfNotExists("fresh_grocery_products", 384)).thenReturn(true);
        when(qdrantService.upsertBatch(eq("fresh_grocery_products"), any())).thenReturn(true);

        SyncVectorResponse syncResp = semanticSearchService.syncAllProducts();

        assertNotNull(syncResp);
        assertEquals(1, syncResp.getTotalProcessed());
        assertEquals(1, syncResp.getTotalSynced());
        assertEquals("fresh_grocery_products", syncResp.getCollectionName());
        verify(qdrantService).createCollectionIfNotExists("fresh_grocery_products", 384);
    }

    @Test
    public void testGetQdrantStatus() {
        when(qdrantService.isHealthy()).thenReturn(true);
        when(qdrantService.getPointsCount("fresh_grocery_products")).thenReturn(12L);
        when(qdrantService.getDashboardUrl()).thenReturn("http://localhost:6333/dashboard");

        QdrantStatusResponse status = semanticSearchService.getStatus();

        assertNotNull(status);
        assertEquals("ONLINE", status.getStatus());
        assertEquals(12L, status.getPointsCount());
        assertEquals(384, status.getVectorDimension());
        assertEquals("http://localhost:6333/dashboard", status.getQdrantDashboardUrl());
    }
}
