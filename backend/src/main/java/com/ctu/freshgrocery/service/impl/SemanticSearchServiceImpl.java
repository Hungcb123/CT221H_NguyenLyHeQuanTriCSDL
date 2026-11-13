package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.exception.AppException;
import org.springframework.http.HttpStatus;
import com.ctu.freshgrocery.dto.HybridSearchResponse;
import com.ctu.freshgrocery.dto.QdrantStatusResponse;
import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.ctu.freshgrocery.dto.SyncVectorResponse;
import com.ctu.freshgrocery.model.Category;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.repository.CategoryRepository;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.service.EmbeddingService;
import com.ctu.freshgrocery.service.QdrantService;
import com.ctu.freshgrocery.service.SemanticSearchService;
import com.ctu.freshgrocery.model.PromotionDetail;
import com.ctu.freshgrocery.repository.PromotionDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PromotionDetailRepository promotionDetailRepository;

    @Value("${qdrant.collection-name:fresh_grocery_products}")
    private String collectionName;

    @Value("${qdrant.dimension:384}")
    private int dimension;

    @Value("${search.semantic-primary-min-score:0.60}")
    private float primaryMinScore = 0.60f;

    @Value("${search.semantic-primary-min-margin:0.05}")
    private float primaryMinMargin = 0.05f;

    @Override
    public List<SemanticProductResponse> search(String query, int limit, Float minScore) {
        return searchWithStatus(query, limit, minScore).getProducts();
    }

    @Override
    public HybridSearchResponse searchWithStatus(String query, int limit, Float minScore) {
        int targetLimit = Math.min(limit > 0 ? limit : 5, 100);
        float threshold = minScore != null ? minScore : 0.35f;
        String cleanQuery = ProductBm25.normalize(query);
        if (cleanQuery.isBlank()) {
            return HybridSearchResponse.builder().products(List.of()).mode("HYBRID")
                    .semanticWeight(0.5f).keywordWeight(0.5f).build();
        }

        // The small catalog is loaded once from SQL, independently of the AI service.
        // SQL failure propagates: a failed database query must never look like no matches.
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getTrangThaiXoa()) && p.getMaSPCha() == null).toList();
        Map<Integer, String> categoryNames = getCategoryNameMap();
        Map<Integer, Float> vectorScores = new HashMap<>();
        boolean semanticAvailable = true;
        try {
            float[] queryVector = embeddingService.generateEmbedding(query);
            for (QdrantService.QdrantScoredPoint point : qdrantService.search(
                    collectionName, queryVector, Math.max(products.size(), targetLimit), threshold)) {
                float score = point.getScore();
                if (Float.isFinite(score) && score >= threshold) {
                    vectorScores.merge(Math.toIntExact(point.getId()), Math.clamp(score, 0f, 1f), Math::max);
                }
            }
        } catch (Exception error) {
            semanticAvailable = false;
            vectorScores.clear();
            log.warn("Semantic search unavailable; using BM25 over real SQL products: {}", error.getMessage());
        }

        float semanticWeight = semanticAvailable ? 0.5f : 0f;
        float keywordWeight = semanticAvailable ? 0.5f : 1f;
        Map<Integer, Float> keywordScores = ProductBm25.score(products, query);
        // Fuse ranks, never raw BM25 and cosine scores (their scales differ).
        Map<Integer, Integer> keywordRanks = ranks(keywordScores);
        Map<Integer, Integer> vectorRanks = ranks(vectorScores);
        List<SemanticProductResponse> results = new ArrayList<>();
        for (Product product : products) {
            float keywordScore = keywordScores.getOrDefault(product.getMaSP(), 0f);
            float vectorScore = vectorScores.getOrDefault(product.getMaSP(), 0f);
            boolean vectorMatch = vectorRanks.containsKey(product.getMaSP());
            if (!keywordRanks.containsKey(product.getMaSP()) && !vectorMatch) continue;

            String matchType = vectorMatch
                    ? (keywordScore > 0f ? "HYBRID" : "SEMANTIC_VECTOR") : "EXACT_KEYWORD";
            SemanticProductResponse result = toResponse(product,
                    categoryNames.get(product.getMaDanhMuc()), vectorScore, matchType);
            result.setKeywordScore(keywordScore);
            result.setRankingScore(rrf(vectorRanks.get(product.getMaSP()), semanticWeight)
                    + rrf(keywordRanks.get(product.getMaSP()), keywordWeight));
            results.add(result);
        }
        results.sort(Comparator.<SemanticProductResponse, Boolean>comparing(result ->
                        (" " + ProductBm25.normalize(result.getTenSP()) + " ").contains(" " + cleanQuery + " "))
                .reversed()
                .thenComparing(Comparator.comparing(SemanticProductResponse::getRankingScore).reversed())
                .thenComparing(SemanticProductResponse::getMaSP));
        var requiredTerms = ProductSearchEvidence.requiredTerms(query);
        var intent = new ProductQueryIntent(query);
        boolean namedTypeMissing = intent.requestedTypeIsMissing(products);
        List<Float> sortedScores = vectorScores.values().stream().sorted(Comparator.reverseOrder()).toList();
        float bestScore = sortedScores.isEmpty() ? 0 : sortedScores.getFirst();
        float runnerUp = sortedScores.size() > 1 ? sortedScores.get(1) : 0;
        // These are conservative, configurable rejection heuristics, not confidence probabilities.
        boolean clearSemanticWinner = semanticAvailable && intent.allowsSemanticOnly()
                && bestScore >= Math.max(threshold, primaryMinScore)
                && bestScore - runnerUp >= primaryMinMargin;
        java.util.function.Predicate<SemanticProductResponse> primary = product -> {
            if (namedTypeMissing || !intent.permits(product)) return false;
            if (ProductSearchEvidence.matches(requiredTerms, product)) return true;
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
                        .thenComparingInt(Integer::intValue)).limit(50).toList();
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
        log.info("Bắt đầu đồng bộ toàn bộ sản phẩm từ MS SQL Server sang CSDL Vector Qdrant...");

        // Đảm bảo collection Qdrant tồn tại
        if (!qdrantService.createCollectionIfNotExists(collectionName, dimension)) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể khởi tạo chỉ mục tìm kiếm ngữ nghĩa.");
        }

        List<Product> products = productRepository.findAll().stream()
                .filter(p -> Boolean.FALSE.equals(p.getTrangThaiXoa()))
                .collect(Collectors.toList());

        Map<Integer, String> categoryNames = getCategoryNameMap();
        List<QdrantService.QdrantPointDto> points = new ArrayList<>();

        for (Product p : products) {
            String semanticDoc = buildSemanticDocument(p, categoryNames.get(p.getMaDanhMuc()));
            float[] vector = embeddingService.generateEmbedding(semanticDoc);

            Map<String, Object> payload = Map.of(
                    "maSP", p.getMaSP(),
                    "tenSP", p.getTenSP(),
                    "gia", p.getGia().doubleValue(),
                    "donViTinh", p.getDonViTinh(),
                    "soLuongTon", p.getSoLuongTon(),
                    "maDanhMuc", p.getMaDanhMuc(),
                    "tenDanhMuc", categoryNames.getOrDefault(p.getMaDanhMuc(), "")
            );

            points.add(QdrantService.QdrantPointDto.builder()
                    .id(p.getMaSP().longValue())
                    .vector(vector)
                    .payload(payload)
                    .build());
        }

        boolean success = qdrantService.upsertBatch(collectionName, points);
        if (!success) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể đồng bộ sản phẩm sang chỉ mục tìm kiếm ngữ nghĩa.");
        }
        long duration = System.currentTimeMillis() - startTime;

        log.info("Hoàn tất đồng bộ vector: {}/{} sản phẩm nạp vào Qdrant trong {} ms",
                points.size(), products.size(), duration);

        return SyncVectorResponse.builder()
                .totalProcessed(products.size())
                .totalSynced(success ? points.size() : 0)
                .collectionName(collectionName)
                .durationMs(duration)
                .message(success
                        ? String.format("Đồng bộ thành công %d sản phẩm sang CSDL Vector Qdrant!", points.size())
                        : "Quá trình nạp vector vào Qdrant gặp lỗi hoặc chưa hoàn tất.")
                .build();
    }

    @Override
    public void syncProduct(Product p) {
        if (p == null || Boolean.TRUE.equals(p.getTrangThaiXoa())) {
            if (p != null) removeProduct(p.getMaSP());
            return;
        }

        try {
            qdrantService.createCollectionIfNotExists(collectionName, dimension);

            Map<Integer, String> categoryNames = getCategoryNameMap();
            String semanticDoc = buildSemanticDocument(p, categoryNames.get(p.getMaDanhMuc()));
            float[] vector = embeddingService.generateEmbedding(semanticDoc);

            Map<String, Object> payload = Map.of(
                    "maSP", p.getMaSP(),
                    "tenSP", p.getTenSP(),
                    "gia", p.getGia().doubleValue(),
                    "donViTinh", p.getDonViTinh(),
                    "soLuongTon", p.getSoLuongTon(),
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
                .modelName("all-MiniLM-L6-v2 (Microsoft ONNX Runtime JVM)")
                .distanceMetric("Cosine")
                .qdrantDashboardUrl(qdrantService.getDashboardUrl())
                .build();
    }

    private String buildSemanticDocument(Product p, String categoryName) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.getTenSP()).append(". ");
        if (p.getMoTa() != null && !p.getMoTa().isBlank()) {
            sb.append(p.getMoTa()).append(". ");
        }
        if (categoryName != null) {
            sb.append("Danh mục: ").append(categoryName).append(". ");
        }

        // Tăng cường ngữ cảnh ẩm thực và ứng dụng thực phẩm
        String nameLower = p.getTenSP().toLowerCase();
        if (nameLower.contains("cần tây")) {
            sb.append("celery rau cu sach nuoc ep thanh loc co the detox giam can giam mo ep rau cu tuoi mat ");
        } else if (nameLower.contains("táo xanh")) {
            sb.append("green apple trai cay hoa qua nuoc ep thanh loc giai khat vitamin c giam mo detox giam can ");
        } else if (nameLower.contains("cá lóc")) {
            sb.append("snakehead fish thuy hai san tuoi song ca dong nau canh chua ca loc kho to chien gion nguyen lieu canh chua ");
        } else if (nameLower.contains("thịt ba chỉ") || nameLower.contains("ba rọi")) {
            sb.append("pork belly thit heo tuoi sach thit kho tau thit kho trung luoc nuong xao mon man ");
        } else if (nameLower.contains("cà chua")) {
            sb.append("tomato rau cu qua da lat nau canh chua xao salad sot ca chua mon an gia dinh ");
        } else if (nameLower.contains("rau muống")) {
            sb.append("water spinach rau xanh tuoi xao toi luoc nau canh chua rau sach ");
        } else if (nameLower.contains("bắp cải")) {
            sb.append("cabbage rau cu da lat nau canh xao luoc salad thanh dam ");
        } else if (nameLower.contains("tôm sú")) {
            sb.append("fresh shrimp prawn hai san tuoi song tom hap bia nuong lau hai san tiec ");
        } else if (nameLower.contains("mực")) {
            sb.append("fresh squid hai san tuoi song muc xao chua ngot hap gung muc nuong ");
        } else if (nameLower.contains("đậu hũ") || nameLower.contains("đậu phụ")) {
            sb.append("tofu dau hu non thuc pham chay nau canh dau hu chien gion sot ca ");
        } else if (nameLower.contains("gừng")) {
            sb.append("ginger cu gia vi nuoc ep giai cam am bung nau canh kho thit gia vi ");
        } else if (nameLower.contains("trứng gà")) {
            sb.append("chicken eggs trung tuoi sach kho thit chien op la bo duong bua sang ");
        }

        return sb.toString();
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
            List<PromotionDetail> promoList = promotionDetailRepository.findActiveDiscountsForProduct(p.getMaSP(), LocalDateTime.now());
            if (promoList != null && !promoList.isEmpty()) {
                PromotionDetail bestPromo = promoList.get(0);
                giaHienTai = bestPromo.getGiaKhuyenMai();
                dangKhuyenMai = true;
                phanTramGiam = bestPromo.getPhanTramGiam();
                if (bestPromo.getPromotion() != null) {
                    tenKM = bestPromo.getPromotion().getTenKM();
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
