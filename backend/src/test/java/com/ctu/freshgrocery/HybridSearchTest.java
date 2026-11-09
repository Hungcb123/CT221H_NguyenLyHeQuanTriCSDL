package com.ctu.freshgrocery;

import com.ctu.freshgrocery.dto.HybridSearchResponse;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.repository.*;
import com.ctu.freshgrocery.service.*;
import com.ctu.freshgrocery.service.impl.SemanticSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HybridSearchTest {
    EmbeddingService embedding = mock(EmbeddingService.class);
    QdrantService qdrant = mock(QdrantService.class);
    ProductRepository repository = mock(ProductRepository.class);
    CategoryRepository categories = mock(CategoryRepository.class);
    SemanticSearchServiceImpl service = new SemanticSearchServiceImpl(embedding, qdrant, repository,
            categories, mock(PromotionDetailRepository.class));

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "collectionName", "products");
        when(embedding.generateEmbedding(anyString())).thenReturn(new float[]{1f});
    }

    Product product(int id, String name) {
        return Product.builder().maSP(id).tenSP(name).gia(BigDecimal.TEN)
                .maDanhMuc(1).soLuongTon(3).trangThaiXoa(false).build();
    }

    QdrantService.QdrantScoredPoint point(int id, float score) {
        return QdrantService.QdrantScoredPoint.builder().id((long) id).score(score).build();
    }

    @Test
    void regionalPorkSynonymsSurvivePrimaryFilteringWithAndWithoutVectors() {
        when(repository.findAll()).thenReturn(List.of(product(1, "Thịt heo xay"), product(2, "Thịt bò")));
        when(qdrant.search(anyString(), any(), anyInt(), anyFloat()))
                .thenReturn(List.of(point(1, 0.8f), point(2, 0.7f)));
        for (String query : List.of("thịt lợn", "thịt heo", "thit lon")) {
            var response = service.searchWithStatus(query, 12, 0.3f);
            assertEquals(List.of(1), response.getProducts().stream().map(p -> p.getMaSP()).toList());
        }
        when(embedding.generateEmbedding(anyString())).thenThrow(new IllegalStateException("offline"));
        assertEquals(1, service.searchWithStatus("thịt lợn", 12, 0.3f).getProducts().getFirst().getMaSP());
        when(repository.findAll()).thenReturn(List.of(product(1, "Thịt lợn xay"), product(2, "Thịt bò")));
        assertEquals(1, service.searchWithStatus("thịt heo", 12, 0.3f).getProducts().getFirst().getMaSP());
    }

    @Test
    void combinesChannelsEquallyAndDeduplicatesProducts() {
        when(repository.findAll()).thenReturn(List.of(product(1, "Cá lóc"), product(2, "Chuối")));
        when(qdrant.search(anyString(), any(), anyInt(), anyFloat()))
                .thenReturn(List.of(point(2, 0.95f), point(1, 0.6f), point(1, 0.55f)));
        var response = service.searchWithStatus("ca loc", 12, 0.3f);
        assertEquals("HYBRID", response.getMode());
        assertEquals(1, response.getProducts().size());
        assertEquals(1, response.getRelatedProducts().size());
        var top = response.getProducts().getFirst();
        assertEquals(1, top.getMaSP());
        assertEquals(0.6f, top.getSimilarityScore());
        assertTrue(top.getKeywordScore() > 0);
        assertEquals(0.5f / 61 + 0.5f / 62, top.getRankingScore(), 0.0001f);
        assertEquals("HYBRID", top.getMatchType());
        assertEquals(0.5f / 61, response.getRelatedProducts().getFirst().getRankingScore(), 0.0001f);
    }

    @Test
    void embeddingFailureStillFindsRealKeywordMatches() {
        when(repository.findAll()).thenReturn(List.of(product(1, "Cá lóc"), product(2, "Chuối")));
        when(embedding.generateEmbedding(anyString())).thenThrow(new IllegalStateException("model offline"));
        assertFallback(service.searchWithStatus("CA LOC", 12, 0.3f));
        verifyNoInteractions(qdrant);
    }

    @Test
    void qdrantFailureUsesFullKeywordWeight() {
        when(repository.findAll()).thenReturn(List.of(product(1, "Cá lóc"), product(2, "Chuối")));
        when(qdrant.search(anyString(), any(), anyInt(), anyFloat()))
                .thenThrow(new IllegalStateException("qdrant offline"));
        assertFallback(service.searchWithStatus("cá lóc", 12, 0.3f));
    }

    void assertFallback(HybridSearchResponse response) {
        assertEquals("KEYWORD_FALLBACK", response.getMode());
        assertEquals(0f, response.getSemanticWeight());
        assertEquals(1f, response.getKeywordWeight());
        assertEquals(1, response.getProducts().size());
        var result = response.getProducts().getFirst();
        assertEquals(1, result.getMaSP());
        assertEquals(0f, result.getSimilarityScore());
        assertEquals(1f / 61, result.getRankingScore(), 0.0001f);
        assertEquals("EXACT_KEYWORD", result.getMatchType());
    }

    @Test
    void noVectorMatchesStillAllowsKeywordResultsAndFiltersDeletedProducts() {
        Product deleted = product(3, "Cá lóc");
        deleted.setTrangThaiXoa(true);
        when(repository.findAll()).thenReturn(List.of(deleted, product(1, "Cá lóc")));
        var response = service.searchWithStatus("ca loc", 1, 0.9f);
        assertEquals(1, response.getProducts().size());
        assertEquals(1, response.getProducts().getFirst().getMaSP());
        assertEquals(0.5f / 61, response.getProducts().getFirst().getRankingScore(), 0.0001f);
    }

    @Test
    void partialTermsMatchButSubstringsOfWordsDoNot() {
        when(repository.findAll()).thenReturn(List.of(product(1, "Cá lóc"), product(2, "Cam")));
        var response = service.searchWithStatus("cá tươi", 12, 0.3f);
        assertTrue(response.getProducts().isEmpty());
        assertEquals(1, response.getRelatedProducts().size());
        assertEquals(1, response.getRelatedProducts().getFirst().getMaSP());
    }

    @Test
    void strongSemanticWinnerCanBePrimaryWithoutAnyQueryWordMatch() {
        when(repository.findAll()).thenReturn(List.of(product(1, "Chuối"), product(2, "Thịt bò")));
        when(qdrant.search(anyString(), any(), anyInt(), anyFloat()))
                .thenReturn(List.of(point(1, 0.91f), point(2, 0.5f)));
        var response = service.searchWithStatus("gợi ý bữa sáng nhanh", 12, 0.3f);
        assertEquals(1, response.getProducts().size());
        assertEquals(1, response.getProducts().getFirst().getMaSP());
        assertEquals(0f, response.getProducts().getFirst().getKeywordScore());
        assertEquals("SEMANTIC_VECTOR", response.getProducts().getFirst().getMatchType());
    }

    @Test
    void missingNamedTypeCannotBeReplacedEvenByAStrongVectorMatch() {
        when(repository.findAll()).thenReturn(List.of(product(1, "Tôm sú tươi")));
        when(qdrant.search(anyString(), any(), anyInt(), anyFloat()))
                .thenReturn(List.of(point(1, 0.99f)));
        var response = service.searchWithStatus("mực để hấp", 12, 0.3f);
        assertTrue(response.getProducts().isEmpty());
        assertEquals(1, response.getRelatedProducts().size());
    }

    @Test
    void closeVectorScoresAreSuggestionsRatherThanConfidentPrimaryMatches() {
        when(repository.findAll()).thenReturn(List.of(product(1, "Chuối"), product(2, "Thịt bò")));
        when(qdrant.search(anyString(), any(), anyInt(), anyFloat()))
                .thenReturn(List.of(point(1, 0.89f), point(2, 0.87f)));
        var response = service.searchWithStatus("gợi ý bữa sáng nhanh", 12, 0.3f);
        assertTrue(response.getProducts().isEmpty());
        assertEquals(2, response.getRelatedProducts().size());
    }

    @Test
    void fishAndTomatoAreDifferentNamedTypesDespiteAccentFolding() {
        when(repository.findAll()).thenReturn(List.of(product(1, "Cà chua")));
        when(qdrant.search(anyString(), any(), anyInt(), anyFloat()))
                .thenReturn(List.of(point(1, 0.99f)));
        assertTrue(service.searchWithStatus("cá để nấu canh", 12, 0.3f).getProducts().isEmpty());
    }

    @Test
    void sqlFailureIsNotDisguisedAsEmptyResults() {
        when(repository.findAll()).thenThrow(new IllegalStateException("SQL offline"));
        assertThrows(IllegalStateException.class, () -> service.searchWithStatus("cá", 12, 0.3f));
        verifyNoInteractions(embedding, qdrant);
    }
}
