package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.HybridSearchResponse;
import com.ctu.freshgrocery.dto.QdrantStatusResponse;
import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.ctu.freshgrocery.dto.SyncVectorResponse;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.model.Category;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.model.PromotionDetail;
import com.ctu.freshgrocery.model.Recipe;
import com.ctu.freshgrocery.model.RecipeIngredient;
import com.ctu.freshgrocery.repository.CategoryRepository;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.repository.PromotionDetailRepository;
import com.ctu.freshgrocery.repository.RecipeRepository;
import com.ctu.freshgrocery.service.EmbeddingService;
import com.ctu.freshgrocery.service.QdrantService;
import com.ctu.freshgrocery.service.SemanticSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PromotionDetailRepository promotionDetailRepository;
    private final RecipeRepository recipeRepository;

    @Value("${qdrant.collection-name:fresh_grocery_products}")
    private String collectionName;

    @Value("${qdrant.dimension:384}")
    private int dimension;

    @Value("${search.semantic-primary-min-score:0.60}")
    private float primaryMinScore = 0.60f;

    @Value("${search.semantic-primary-min-margin:0.05}")
    private float primaryMinMargin = 0.05f;

    public SemanticSearchServiceImpl(
            EmbeddingService embeddingService,
            QdrantService qdrantService,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            PromotionDetailRepository promotionDetailRepository) {
        this(embeddingService, qdrantService, productRepository, categoryRepository, promotionDetailRepository, null);
    }

    @Autowired
    public SemanticSearchServiceImpl(
            EmbeddingService embeddingService,
            QdrantService qdrantService,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            PromotionDetailRepository promotionDetailRepository,
            RecipeRepository recipeRepository) {
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.promotionDetailRepository = promotionDetailRepository;
        this.recipeRepository = recipeRepository;
    }

    @Override
    public List<SemanticProductResponse> search(String query, int limit, Float minScore) {
        return searchWithStatus(query, limit, minScore).getProducts();
    }

    @Override
    public HybridSearchResponse searchWithStatus(String query, int limit, Float minScore) {
        int targetLimit = Math.min(limit > 0 ? limit : 5, 100);
        float threshold = minScore != null ? minScore : 0.35f;
        String rawCleanQuery = ProductBm25.normalize(query);
        if (rawCleanQuery.isBlank()) {
            return HybridSearchResponse.builder().products(List.of()).mode("HYBRID")
                    .semanticWeight(0.5f).keywordWeight(0.5f).build();
        }

        // Load active storefront parent products once from SQL.
        // SQL failure propagates: a failed database query must never look like no matches.
        boolean allowTestProducts = VietnameseFoodSemantics.queryRequestsTestProduct(query);
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getTrangThaiXoa()) && p.getMaSPCha() == null)
                .filter(p -> allowTestProducts || !VietnameseFoodSemantics.isInternalTestProduct(p))
                .toList();
        Set<Integer> parentIds = products.stream().map(Product::getMaSP).filter(Objects::nonNull)
                .collect(Collectors.toSet());

        String resolvedQuery = VietnameseFoodSemantics.resolveTypoQuery(query, products);
        String cleanQuery = ProductBm25.normalize(resolvedQuery);

        Map<Integer, String> categoryNames = getCategoryNameMap();
        Set<Integer> recipeMatchedIds = findMatchingRecipeProductIds(resolvedQuery);
        boolean hasConcepts = !VietnameseFoodSemantics.matchedConcepts(resolvedQuery).isEmpty();

        Map<Integer, Float> vectorScores = new HashMap<>();
        boolean semanticAvailable = true;
        try {
            String embeddingQuery = VietnameseFoodSemantics.buildQueryEmbeddingText(resolvedQuery);
            float[] queryVector = embeddingService.generateEmbedding(embeddingQuery);
            for (QdrantService.QdrantScoredPoint point : qdrantService.search(
                    collectionName, queryVector, Math.max(products.size() * 2, targetLimit), threshold)) {
                float score = point.getScore();
                int pointId = Math.toIntExact(point.getId());
                if (parentIds.contains(pointId) && Float.isFinite(score) && score >= threshold) {
                    vectorScores.merge(pointId, Math.clamp(score, 0f, 1f), Math::max);
                }
            }
        } catch (Exception error) {
            semanticAvailable = false;
            vectorScores.clear();
            log.warn("Semantic search unavailable; using BM25 over real SQL products: {}", error.getMessage());
        }

        float semanticWeight = semanticAvailable ? 0.5f : 0f;
        float keywordWeight = semanticAvailable ? 0.5f : 1f;
        Map<Integer, Float> keywordScores = ProductBm25.score(products, resolvedQuery);
        // Fuse ranks, never raw BM25 and cosine scores (their scales differ).
        Map<Integer, Integer> keywordRanks = ranks(keywordScores);
        Map<Integer, Integer> vectorRanks = ranks(vectorScores);

        var intent = new ProductQueryIntent(resolvedQuery);
        List<SemanticProductResponse> results = new ArrayList<>();
        for (Product product : products) {
            float keywordScore = keywordScores.getOrDefault(product.getMaSP(), 0f);
            float vectorScore = vectorScores.getOrDefault(product.getMaSP(), 0f);
            boolean vectorMatch = vectorRanks.containsKey(product.getMaSP());
            String catName = categoryNames.get(product.getMaDanhMuc());

            SemanticProductResponse preview = SemanticProductResponse.builder()
                    .maSP(product.getMaSP())
                    .tenSP(product.getTenSP())
                    .moTa(product.getMoTa())
                    .tenDanhMuc(catName)
                    .build();
            boolean culinaryMatch = (recipeMatchedIds.contains(product.getMaSP()) || hasConcepts)
                    && intent.permits(preview)
                    && VietnameseFoodSemantics.matchesCulinaryOrRecipeConcept(resolvedQuery, preview, recipeMatchedIds);

            if (!keywordRanks.containsKey(product.getMaSP()) && !vectorMatch && !culinaryMatch) continue;

            String matchType = vectorMatch
                    ? (keywordScore > 0f ? "HYBRID" : "SEMANTIC_VECTOR")
                    : (keywordScore > 0f ? "EXACT_KEYWORD" : (semanticAvailable ? "SEMANTIC_VECTOR" : "EXACT_KEYWORD"));
            SemanticProductResponse result = toResponse(product, catName, vectorScore, matchType);
            result.setKeywordScore(keywordScore);
            result.setRankingScore(rrf(vectorRanks.get(product.getMaSP()), semanticWeight)
                    + rrf(keywordRanks.get(product.getMaSP()), keywordWeight));
            results.add(result);
        }

        results.sort(Comparator.<SemanticProductResponse, Boolean>comparing(result ->
                        (" " + ProductBm25.normalize(result.getTenSP()) + " ").contains(" " + cleanQuery + " "))
                .reversed()
                .thenComparing(Comparator.<SemanticProductResponse, Boolean>comparing(result ->
                        recipeMatchedIds.contains(result.getMaSP())).reversed())
                .thenComparing(Comparator.comparing(SemanticProductResponse::getRankingScore).reversed())
                .thenComparing(SemanticProductResponse::getMaSP));

        var requiredTerms = ProductSearchEvidence.requiredTerms(resolvedQuery);
        boolean namedTypeMissing = intent.requestedTypeIsMissing(products);
        List<Float> sortedScores = vectorScores.values().stream().sorted(Comparator.reverseOrder()).toList();
        float bestScore = sortedScores.isEmpty() ? 0 : sortedScores.getFirst();
        float runnerUp = sortedScores.size() > 1 ? sortedScores.get(1) : 0;
        // Conservative rejection heuristic for pure vector-only admission when no lexical/culinary evidence exists.
        boolean clearSemanticWinner = semanticAvailable && intent.allowsSemanticOnly()
                && bestScore >= Math.max(threshold, primaryMinScore)
                && bestScore - runnerUp >= primaryMinMargin;

        java.util.function.Predicate<SemanticProductResponse> primary = product -> {
            if (namedTypeMissing || !intent.permits(product)) return false;
            if (ProductSearchEvidence.matches(requiredTerms, product)) return true;
            if (intent.isMultiProductList()) return true;
            if (VietnameseFoodSemantics.matchesCulinaryOrRecipeConcept(resolvedQuery, product, recipeMatchedIds)) {
                return true;
            }
            return clearSemanticWinner && vectorRanks.containsKey(product.getMaSP())
                    && vectorScores.get(product.getMaSP()) >= bestScore;
        };

        return HybridSearchResponse.builder()
                .products(results.stream().filter(primary).limit(targetLimit).toList())
                .relatedProducts(results.stream().filter(primary.negate())
                        .limit(Math.min(targetLimit, 6)).toList())
                .mode(semanticAvailable ? "HYBRID" : "KEYWORD_FALLBACK")
                .semanticWeight(semanticWeight).keywordWeight(keywordWeight).build();
    }

    private Map<Integer, Integer> ranks(Map<Integer, Float> scores) {
        List<Integer> ids = scores.keySet().stream()
                .sorted(Comparator.<Integer, Float>comparing(scores::get).reversed()
                        .thenComparingInt(Integer::intValue)).limit(80).toList();
        Map<Integer, Integer> ranks = new HashMap<>();
        for (int index = 0; index < ids.size(); index++) ranks.put(ids.get(index), index + 1);
        return ranks;
    }

    private float rrf(Integer rank, float weight) {
        return rank == null ? 0f : weight / (60f + rank);
    }

    @Override
    public SyncVectorResponse syncAllProducts() {
        long startTime = System.currentTimeMillis();
        log.info("Bắt đầu đồng bộ toàn bộ sản phẩm cha từ MS SQL Server sang CSDL Vector Qdrant...");

        if (!qdrantService.createCollectionIfNotExists(collectionName, dimension)) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể khởi tạo chỉ mục tìm kiếm ngữ nghĩa.");
        }

        List<Product> products = productRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getTrangThaiXoa()) && p.getMaSPCha() == null)
                .filter(p -> !VietnameseFoodSemantics.isInternalTestProduct(p))
                .collect(Collectors.toList());

        Map<Integer, String> categoryNames = getCategoryNameMap();
        Map<Integer, List<String>> productRecipeNames = getProductRecipeNamesMap();
        List<QdrantService.QdrantPointDto> points = new ArrayList<>();
        Set<Long> activePointIds = new HashSet<>();

        for (Product p : products) {
            String semanticDoc = VietnameseFoodSemantics.buildSemanticDocument(
                    p, categoryNames.get(p.getMaDanhMuc()), productRecipeNames.getOrDefault(p.getMaSP(), List.of()));
            float[] vector = embeddingService.generateEmbedding(semanticDoc);

            Map<String, Object> payload = Map.of(
                    "maSP", p.getMaSP(),
                    "tenSP", p.getTenSP(),
                    "gia", p.getGia().doubleValue(),
                    "donViTinh", Objects.toString(p.getDonViTinh(), ""),
                    "soLuongTon", Objects.toString(p.getSoLuongTon(), "0"),
                    "maDanhMuc", p.getMaDanhMuc(),
                    "tenDanhMuc", categoryNames.getOrDefault(p.getMaDanhMuc(), "")
            );

            long pointId = p.getMaSP().longValue();
            activePointIds.add(pointId);
            points.add(QdrantService.QdrantPointDto.builder()
                    .id(pointId)
                    .vector(vector)
                    .payload(payload)
                    .build());
        }

        boolean success = qdrantService.upsertBatch(collectionName, points);
        if (!success) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể đồng bộ sản phẩm sang chỉ mục tìm kiếm ngữ nghĩa.");
        }
        qdrantService.retainOnlyPoints(collectionName, activePointIds);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Hoàn tất đồng bộ vector: {}/{} sản phẩm nạp vào Qdrant trong {} ms",
                points.size(), products.size(), duration);

        return SyncVectorResponse.builder()
                .totalProcessed(products.size())
                .totalSynced(points.size())
                .collectionName(collectionName)
                .durationMs(duration)
                .message(String.format("Đồng bộ thành công %d sản phẩm sang CSDL Vector Qdrant!", points.size()))
                .build();
    }

    @Override
    public void syncProduct(Product p) {
        if (p == null || Boolean.TRUE.equals(p.getTrangThaiXoa())
                || p.getMaSPCha() != null || VietnameseFoodSemantics.isInternalTestProduct(p)) {
            if (p != null && p.getMaSP() != null) removeProduct(p.getMaSP());
            return;
        }

        try {
            qdrantService.createCollectionIfNotExists(collectionName, dimension);

            Map<Integer, String> categoryNames = getCategoryNameMap();
            Map<Integer, List<String>> productRecipeNames = getProductRecipeNamesMap();
            String semanticDoc = VietnameseFoodSemantics.buildSemanticDocument(
                    p, categoryNames.get(p.getMaDanhMuc()), productRecipeNames.getOrDefault(p.getMaSP(), List.of()));
            float[] vector = embeddingService.generateEmbedding(semanticDoc);

            Map<String, Object> payload = Map.of(
                    "maSP", p.getMaSP(),
                    "tenSP", p.getTenSP(),
                    "gia", p.getGia().doubleValue(),
                    "donViTinh", Objects.toString(p.getDonViTinh(), ""),
                    "soLuongTon", Objects.toString(p.getSoLuongTon(), "0"),
                    "maDanhMuc", p.getMaDanhMuc(),
                    "tenDanhMuc", categoryNames.getOrDefault(p.getMaDanhMuc(), "")
            );

            qdrantService.upsertPoint(collectionName, p.getMaSP().longValue(), vector, payload);
            log.info("Đã đồng bộ vector cho sản phẩm ID={}: [{}] vào Qdrant", p.getMaSP(), p.getTenSP());
        } catch (Exception e) {
            log.warn("Không thể đồng bộ vector cho sản phẩm ID={}: {}", p.getMaSP(), e.getMessage());
        }
    }

    @Override
    public void removeProduct(Integer productId) {
        if (productId != null) {
            try {
                qdrantService.deletePoint(collectionName, productId.longValue());
                log.info("Đã xóa vector sản phẩm ID={} khỏi Qdrant", productId);
            } catch (Exception e) {
                log.warn("Lỗi khi xóa vector sản phẩm ID={} khỏi Qdrant: {}", productId, e.getMessage());
            }
        }
    }

    @Override
    public QdrantStatusResponse getStatus() {
        boolean healthy = qdrantService.isHealthy();
        Long count = healthy ? qdrantService.getPointsCount(collectionName) : 0L;

        return QdrantStatusResponse.builder()
                .status(healthy ? "ONLINE" : "OFFLINE / UNREACHABLE")
                .collectionName(collectionName)
                .pointsCount(count)
                .vectorDimension(dimension)
                .modelName("all-MiniLM-L6-v2 + Bilingual Culinary Bridge (ONNX JVM)")
                .distanceMetric("Cosine")
                .qdrantDashboardUrl(qdrantService.getDashboardUrl())
                .build();
    }

    private Set<Integer> findMatchingRecipeProductIds(String query) {
        if (recipeRepository == null || query == null || query.isBlank()
                || VietnameseFoodSemantics.hasNegation(query)) {
            return Set.of();
        }
        try {
            var coreTerms = ProductSearchEvidence.requiredTerms(query);
            if (coreTerms.isEmpty()) return Set.of();
            String corePhrase = String.join(" ", coreTerms);
            String queryNorm = ProductBm25.normalize(query);

            Set<Integer> matchedProductIds = new LinkedHashSet<>();
            for (Recipe recipe : recipeRepository.findAllWithIngredients()) {
                if (recipe.getTenMonAn() == null) continue;
                String recipeNorm = ProductBm25.normalize(recipe.getTenMonAn());
                if (recipeNorm.isBlank()) continue;

                boolean directPhrase = coreTerms.size() >= 2
                        && ((" " + recipeNorm + " ").contains(" " + corePhrase + " ")
                        || (" " + queryNorm + " ").contains(" " + recipeNorm + " "));

                boolean dishKeywordMatch = false;
                for (String dishKey : List.of(
                        "canh chua", "kho tau", "kho to", "pho bo", "lau hai san",
                        "bo luc lac", "goi cuon", "nuoc ep can tay", "canh bi do",
                        "bong cai xao", "muc xao", "ngheu hap", "ca dieu hong hap")) {
                    if ((" " + queryNorm + " ").contains(" " + dishKey + " ")
                            && (" " + recipeNorm + " ").contains(" " + dishKey + " ")) {
                        dishKeywordMatch = true;
                        break;
                    }
                }

                if (directPhrase || dishKeywordMatch) {
                    if (recipe.getIngredients() != null) {
                        for (RecipeIngredient ing : recipe.getIngredients()) {
                            if (ing.getId() != null && ing.getId().getMaSP() != null) {
                                matchedProductIds.add(ing.getId().getMaSP());
                            }
                        }
                    }
                }
            }
            return matchedProductIds;
        } catch (Exception e) {
            log.debug("Không thể truy vấn công thức món ăn cho tìm kiếm: {}", e.getMessage());
            return Set.of();
        }
    }

    private Map<Integer, List<String>> getProductRecipeNamesMap() {
        if (recipeRepository == null) return Map.of();
        try {
            Map<Integer, List<String>> map = new HashMap<>();
            for (Recipe recipe : recipeRepository.findAllWithIngredients()) {
                if (recipe.getTenMonAn() == null || recipe.getIngredients() == null) continue;
                for (RecipeIngredient ing : recipe.getIngredients()) {
                    if (ing.getId() != null && ing.getId().getMaSP() != null) {
                        map.computeIfAbsent(ing.getId().getMaSP(), k -> new ArrayList<>())
                                .add(recipe.getTenMonAn());
                    }
                }
            }
            return map;
        } catch (Exception e) {
            log.debug("Không thể tải danh sách công thức món ăn khi đồng bộ vector: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<Integer, String> getCategoryNameMap() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getMaDanhMuc, Category::getTenDanhMuc, (a, b) -> a));
    }

    private SemanticProductResponse toResponse(Product p, String catName, Float score, String matchType) {
        BigDecimal giaGoc = p.getGia();
        BigDecimal giaHienTai = p.getGia();
        boolean dangKhuyenMai = false;
        BigDecimal phanTramGiam = BigDecimal.ZERO;
        String tenKM = null;
        Integer maKM = null;

        try {
            boolean family = productRepository.existsByMaSPCha(p.getMaSP());
            if (family) phanTramGiam = null;
            List<PromotionDetail> promoList = family ? List.of()
                    : promotionDetailRepository.findActiveDiscountsForProduct(p.getMaSP(), LocalDateTime.now());
            if (promoList != null && !promoList.isEmpty()) {
                PromotionDetail bestPromo = promoList.get(0);
                giaHienTai = bestPromo.getGiaKhuyenMai();
                dangKhuyenMai = true;
                phanTramGiam = bestPromo.getPhanTramGiam();
                if (bestPromo.getPromotion() != null) {
                    tenKM = bestPromo.getPromotion().getMaKM() != null ? bestPromo.getPromotion().getTenKM() : null;
                    maKM = bestPromo.getPromotion().getMaKM();
                }
            }
        } catch (Exception e) {
            log.warn("Không thể tải thông tin khuyến mãi cho sản phẩm #{}: {}", p.getMaSP(), e.getMessage());
        }

        return SemanticProductResponse.builder()
                .hinhAnh(p.getHinhAnh())
                .maSP(p.getMaSP())
                .tenSP(p.getTenSP())
                .moTa(p.getMoTa())
                .gia(giaHienTai)
                .giaGoc(giaGoc)
                .giaHienTai(giaHienTai)
                .dangKhuyenMai(dangKhuyenMai)
                .phanTramGiam(phanTramGiam)
                .tenChuongTrinhKM(tenKM)
                .maKM(maKM)
                .donViTinh(p.getDonViTinh())
                .soLuongTon(p.getSoLuongTon())
                .hanSuDung(p.getHanSuDung())
                .nhietDoBaoQuan(p.getNhietDoBaoQuan())
                .maDanhMuc(p.getMaDanhMuc())
                .tenDanhMuc(catName)
                .similarityScore(Math.round(score * 1000f) / 1000f)
                .matchType(matchType)
                .build();
    }
}
