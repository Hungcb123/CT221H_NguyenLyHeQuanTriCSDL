package com.ctu.freshgrocery;

import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.ctu.freshgrocery.model.Category;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.model.Recipe;
import com.ctu.freshgrocery.model.RecipeIngredient;
import com.ctu.freshgrocery.model.RecipeIngredientId;
import com.ctu.freshgrocery.repository.CategoryRepository;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.repository.PromotionDetailRepository;
import com.ctu.freshgrocery.repository.RecipeRepository;
import com.ctu.freshgrocery.service.EmbeddingService;
import com.ctu.freshgrocery.service.QdrantService;
import com.ctu.freshgrocery.service.impl.EmbeddingServiceImpl;
import com.ctu.freshgrocery.service.impl.ProductQueryIntent;
import com.ctu.freshgrocery.service.impl.SemanticSearchServiceImpl;
import com.ctu.freshgrocery.service.impl.VietnameseFoodSemantics;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VietnameseFoodSemanticsTest {

    private Product item(int id, String name, String desc, int catId) {
        return Product.builder()
                .maSP(id).tenSP(name).moTa(desc).maDanhMuc(catId)
                .gia(BigDecimal.valueOf(30000)).soLuongTon(20).trangThaiXoa(false)
                .build();
    }

    @Test
    void repairsCommonVietnameseSearchTyposWithoutMutatingValidQueries() {
        List<Product> catalog = List.of(
                item(1, "Cá hồi phi lê Nauy tươi", "Giàu Omega-3", 3),
                item(2, "Cá lóc tươi làm sạch", "Nấu canh chua", 3),
                item(3, "Tôm sú tươi sống", "Hấp bia", 3),
                item(4, "Rau muống nước sạch", "Xào tỏi", 1)
        );
        assertEquals("ca hoi", VietnameseFoodSemantics.resolveTypoQuery("ca hoii", catalog));
        assertEquals("ca loc", VietnameseFoodSemantics.resolveTypoQuery("ca looc", catalog));
        assertEquals("tom su", VietnameseFoodSemantics.resolveTypoQuery("tom suu", catalog));
        assertEquals("rau muong", VietnameseFoodSemantics.resolveTypoQuery("rau muon", catalog));
        assertEquals("sữa tươi không đường",
                VietnameseFoodSemantics.resolveTypoQuery("sữa tươi không đường", catalog));
        assertEquals("thịt dê", VietnameseFoodSemantics.resolveTypoQuery("thịt dê", catalog));
    }

    @Test
    void multiIngredientShoppingListPermitsAllMentionedItemsWhileKeepingSpanSuppression() {
        var multiIntent = new ProductQueryIntent("hành tỏi ớt gừng sả");
        assertTrue(multiIntent.isMultiProductList());
        assertTrue(multiIntent.permits(SemanticProductResponse.builder().tenSP("Hành lá tươi").build()));
        assertTrue(multiIntent.permits(SemanticProductResponse.builder().tenSP("Tỏi ta Lý Sơn 250g").build()));
        assertTrue(multiIntent.permits(SemanticProductResponse.builder().tenSP("Ớt hiểm đỏ tươi 100g").build()));
        assertTrue(multiIntent.permits(SemanticProductResponse.builder().tenSP("Gừng tươi rửa sạch 250g").build()));
        assertTrue(multiIntent.permits(SemanticProductResponse.builder().tenSP("Sả cây tươi 300g").build()));
        assertFalse(multiIntent.permits(SemanticProductResponse.builder().tenSP("Cá hồi phi lê").build()));

        var culantro = new ProductQueryIntent("rau mùi tàu");
        assertFalse(culantro.isMultiProductList());
        assertTrue(culantro.permits(SemanticProductResponse.builder().tenSP("Ngò gai tươi").build()));
        assertFalse(culantro.permits(SemanticProductResponse.builder().tenSP("Ngò rí tươi").build()));
    }

    @Test
    void categoryScopeKeepsFruitVitaminQueryStrictlyToFruitsWhenCategoriesExist() {
        var intent = new ProductQueryIntent("trái cây giàu vitamin C");
        assertTrue(intent.permits(SemanticProductResponse.builder()
                .tenSP("Cam sành miền Tây").tenDanhMuc("Trái cây tươi nhiệt đới").build()));
        assertFalse(intent.permits(SemanticProductResponse.builder()
                .tenSP("Cà chua tươi Đà Lạt").tenDanhMuc("Rau củ quả tươi sạch").build()));
    }

    @Test
    void excludesInternalTestItemsAndMatchesRecipeAndNutritionQueries() {
        EmbeddingService embedding = mock(EmbeddingService.class);
        QdrantService qdrant = mock(QdrantService.class);
        ProductRepository productRepo = mock(ProductRepository.class);
        CategoryRepository catRepo = mock(CategoryRepository.class);
        RecipeRepository recipeRepo = mock(RecipeRepository.class);
        when(embedding.generateEmbedding(anyString())).thenReturn(new float[]{1f});

        Product fish = item(1, "Cá lóc tươi làm sạch", "Nguyên liệu nấu canh chua cá lóc miền Tây", 3);
        Product tomato = item(2, "Cà chua tươi Đà Lạt", "Nấu canh chua miền Tây, làm sốt cà chua", 1);
        Product chickenBreast = item(3, "Ức gà phi lê", "Thực phẩm giàu protein ít béo cho người tập gym", 2);
        Product testItem = item(99, "[TEST] Gói thử thanh toán 1.000đ", "Sản phẩm DEMO kiểm thử", 1);
        Product childPack = item(100, "Ức gà phi lê - 300g", "Quy cách 300g", 2);
        childPack.setMaSPCha(3);

        when(productRepo.findAll()).thenReturn(List.of(fish, tomato, chickenBreast, testItem, childPack));
        when(catRepo.findAll()).thenReturn(List.of(
                Category.builder().maDanhMuc(1).tenDanhMuc("Rau củ quả tươi sạch").build(),
                Category.builder().maDanhMuc(2).tenDanhMuc("Thịt tươi sống mỗi ngày").build(),
                Category.builder().maDanhMuc(3).tenDanhMuc("Thủy hải sản tươi sống").build()
        ));

        Recipe canhChua = Recipe.builder()
                .maCongThuc(1).tenMonAn("Canh chua cá lóc miền Tây")
                .ingredients(List.of(
                        RecipeIngredient.builder().id(new RecipeIngredientId(1, 1)).build(),
                        RecipeIngredient.builder().id(new RecipeIngredientId(1, 2)).build()
                )).build();
        when(recipeRepo.findAllWithIngredients()).thenReturn(List.of(canhChua));
        when(qdrant.search(anyString(), any(), anyInt(), anyFloat())).thenReturn(List.of(
                QdrantService.QdrantScoredPoint.builder().id(1L).score(0.78f).build(),
                QdrantService.QdrantScoredPoint.builder().id(2L).score(0.74f).build(),
                QdrantService.QdrantScoredPoint.builder().id(3L).score(0.72f).build(),
                QdrantService.QdrantScoredPoint.builder().id(99L).score(0.95f).build(),
                QdrantService.QdrantScoredPoint.builder().id(100L).score(0.95f).build()
        ));
        when(qdrant.createCollectionIfNotExists(anyString(), anyInt())).thenReturn(true);
        when(qdrant.upsertBatch(anyString(), any())).thenReturn(true);

        var service = new SemanticSearchServiceImpl(
                embedding, qdrant, productRepo, catRepo, mock(PromotionDetailRepository.class), recipeRepo);
        ReflectionTestUtils.setField(service, "collectionName", "products");
        ReflectionTestUtils.setField(service, "dimension", 384);

        var soupResult = service.searchWithStatus("nguyên liệu nấu canh chua miền Tây", 12, 0.3f);
        assertEquals(List.of(1, 2), soupResult.getProducts().stream().map(SemanticProductResponse::getMaSP).toList());
        assertTrue(soupResult.getRelatedProducts().stream().noneMatch(p -> p.getMaSP() == 99 || p.getMaSP() == 100));

        var gymResult = service.searchWithStatus("thực phẩm giàu protein tập gym", 12, 0.3f);
        assertEquals(List.of(3), gymResult.getProducts().stream().map(SemanticProductResponse::getMaSP).toList());

        var sync = service.syncAllProducts();
        assertEquals(3, sync.getTotalSynced());
        verify(qdrant).retainOnlyPoints(eq("products"), eq(Set.of(1L, 2L, 3L)));
    }

    @Test
    void bilingualEmbeddingsPlaceVietnameseQueriesCloserToMatchingProducts() {
        EmbeddingServiceImpl realEmbedding = new EmbeddingServiceImpl();
        realEmbedding.init();
        try {
            Product salmon = item(9, "Cá hồi phi lê Nauy tươi",
                    "Thực phẩm giàu protein và Omega-3, làm sashimi hoặc tập gym eat clean", 3);
            Product sugar = item(57, "Đường trắng 1kg", "Đường trắng nấu chè", 5);

            float[] qVec = realEmbedding.generateEmbedding(
                    VietnameseFoodSemantics.buildQueryEmbeddingText("thực phẩm giàu protein và omega 3 tập gym"));
            float[] salmonVec = realEmbedding.generateEmbedding(
                    VietnameseFoodSemantics.buildSemanticDocument(salmon, "Thủy hải sản tươi sống", List.of()));
            float[] sugarVec = realEmbedding.generateEmbedding(
                    VietnameseFoodSemantics.buildSemanticDocument(sugar, "Gia vị & Đồ khô bách hóa", List.of()));

            float simSalmon = dot(qVec, salmonVec);
            float simSugar = dot(qVec, sugarVec);
            assertTrue(simSalmon > 0.55f && simSalmon > simSugar + 0.25f,
                    "Expected strong bilingual cosine similarity for salmon (" + simSalmon + ") vs sugar (" + simSugar + ")");
        } finally {
            realEmbedding.cleanup();
        }
    }

    private static float dot(float[] a, float[] b) {
        float s = 0f;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }
}
