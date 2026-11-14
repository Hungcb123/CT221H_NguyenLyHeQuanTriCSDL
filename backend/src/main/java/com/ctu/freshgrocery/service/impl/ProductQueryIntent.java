package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.ctu.freshgrocery.model.Product;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.text.Normalizer;
import java.util.*;

/**
 * Domain vocabulary & query intent analyzer for named product requests, multi-item lists,
 * category constraints, and culinary/lifestyle search intents.
 */
public final class ProductQueryIntent {
    private static final List<List<String>> TYPES = loadTypes();

    private static final Map<String, List<String>> BARE_INGREDIENT_EXPANSIONS = Map.of(
            "hành", List.of("hành lá", "hành tím"),
            "ớt", List.of("ớt hiểm", "ớt chuông"),
            "sả", List.of("sả cây"),
            "nấm", List.of("nấm kim châm", "nấm đùi gà", "nấm rơm"),
            "bún", List.of("bún gạo"),
            "gạo", List.of("gạo thơm"),
            "đường", List.of("đường trắng"),
            "muối", List.of("muối i ốt", "muối")
    );

    private record SpanMatch(List<String> aliases, int start, int end, int charLen) {
        int tokenLen() { return end - start; }
        boolean overlaps(SpanMatch other) { return this.start < other.end && other.start < this.end; }
    }

    private final List<List<String>> requestedTypes;
    private final boolean multiProductList;
    private final boolean cookingIntent;
    private final String requiredCategoryKeyword;

    public ProductQueryIntent(String query) {
        String safeQuery = Objects.toString(query, "");
        String text = ProductBm25.normalize(safeQuery);
        String canonical = canonical(safeQuery);
        boolean accentedQuery = safeQuery.codePoints().anyMatch(c -> c > 127);

        this.cookingIntent = List.of(
                        "nguyen lieu", "nau", "lam", "mon", "de", "goi y", "bua", "thich hop", "phu hop",
                        "nuoc ep", "detox", "thanh loc", "protein", "giau dam", "vitamin", "tap gym",
                        "eat clean", "an kieng", "an sang", "an chay", "kho tau", "kho to", "luc lac",
                        "goi cuon", "lau", "pho")
                .stream().anyMatch(term -> contains(text, term))
                || !VietnameseFoodSemantics.matchedConcepts(safeQuery).isEmpty();

        this.requestedTypes = extractNonOverlappingTypes(canonical, text, accentedQuery);
        this.multiProductList = detectMultiProductList(canonical, text, accentedQuery, this.requestedTypes);
        this.requiredCategoryKeyword = detectCategoryScope(canonical, text);
    }

    public boolean allowsSemanticOnly() {
        return cookingIntent;
    }

    public boolean isMultiProductList() {
        return multiProductList;
    }

    public boolean permits(SemanticProductResponse product) {
        if (product == null || product.getTenSP() == null) return false;

        // Category scope guard applies only when hydrated category name is present (leaves bare unit-test mocks untouched).
        if (requiredCategoryKeyword != null && product.getTenDanhMuc() != null && !product.getTenDanhMuc().isBlank()) {
            String catNorm = ProductBm25.normalize(product.getTenDanhMuc());
            if (!contains(catNorm, requiredCategoryKeyword)) {
                return false;
            }
        }

        if (requestedTypes.isEmpty()) return true;
        return requestedTypes.stream().anyMatch(group ->
                group.stream().anyMatch(alias -> nameContains(product.getTenSP(), alias)));
    }

    public boolean requestedTypeIsMissing(List<Product> catalog) {
        if (requestedTypes.isEmpty()) return false;
        if (catalog == null || catalog.isEmpty()) return true;

        // Head requested type (first mentioned product entity) must exist unless this is a multi-item shopping list
        // where at least one requested item is available.
        if (multiProductList) {
            return requestedTypes.stream().allMatch(group -> isGroupMissing(group, catalog));
        }
        return isGroupMissing(requestedTypes.getFirst(), catalog);
    }

    private static boolean isGroupMissing(List<String> group, List<Product> catalog) {
        return catalog.stream().noneMatch(product ->
                product.getTenSP() != null && group.stream().anyMatch(alias -> nameContains(product.getTenSP(), alias)));
    }

    private static List<List<String>> extractNonOverlappingTypes(String canonical, String folded, boolean accented) {
        String basis = accented ? canonical : folded;
        if (basis.isBlank()) return List.of();
        String[] tokens = basis.split(" +");

        List<SpanMatch> candidates = new ArrayList<>();
        for (List<String> aliases : TYPES) {
            SpanMatch bestForGroup = null;
            for (String alias : aliases) {
                String aliasForm = accented ? canonical(alias) : ProductBm25.normalize(alias);
                if (aliasForm.isBlank()) continue;
                String[] aliasTokens = aliasForm.split(" +");
                for (int i = 0; i <= tokens.length - aliasTokens.length; i++) {
                    boolean match = true;
                    for (int k = 0; k < aliasTokens.length; k++) {
                        if (!tokens[i + k].equals(aliasTokens[k])) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        SpanMatch candidate = new SpanMatch(aliases, i, i + aliasTokens.length, aliasForm.length());
                        if (bestForGroup == null
                                || candidate.tokenLen() > bestForGroup.tokenLen()
                                || (candidate.tokenLen() == bestForGroup.tokenLen() && candidate.charLen() > bestForGroup.charLen())) {
                            bestForGroup = candidate;
                        }
                    }
                }
            }
            if (bestForGroup != null) {
                candidates.add(bestForGroup);
            }
        }

        // Also check bare accented single-token ingredients (e.g. "hành tỏi ớt gừng sả", "gỏi cuốn tôm thịt")
        if (accented && !candidates.isEmpty()) {
            String[] canonTokens = canonical.split(" +");
            for (int i = 0; i < canonTokens.length; i++) {
                List<String> bareGroup = BARE_INGREDIENT_EXPANSIONS.get(canonTokens[i]);
                if (bareGroup != null) {
                    candidates.add(new SpanMatch(bareGroup, i, i + 1, canonTokens[i].length()));
                } else if ("thịt".equals(canonTokens[i]) && i > 0 && "tôm".equals(canonTokens[i - 1])) {
                    candidates.add(new SpanMatch(List.of("ba rọi", "thịt heo", "nạc"), i, i + 1, 4));
                }
            }
        }

        // Longest non-overlapping span suppression (shadows sub-phrases like "rau mùi" inside "rau mùi tàu",
        // or "cá" inside "cá hồi", while preserving separate entities like "cần tây" + "táo xanh").
        candidates.sort(Comparator.comparingInt(SpanMatch::tokenLen).reversed()
                .thenComparing(Comparator.comparingInt(SpanMatch::charLen).reversed())
                .thenComparingInt(SpanMatch::start));

        List<SpanMatch> accepted = new ArrayList<>();
        for (SpanMatch candidate : candidates) {
            boolean overlaps = accepted.stream().anyMatch(existing -> existing.overlaps(candidate));
            if (!overlaps) {
                accepted.add(candidate);
            }
        }
        accepted.sort(Comparator.comparingInt(SpanMatch::start));
        return accepted.stream().map(SpanMatch::aliases).toList();
    }

    private static boolean detectMultiProductList(
            String canonical, String folded, boolean accented, List<List<String>> requestedTypes) {
        if (requestedTypes.size() < 2) return false;
        if (VietnameseFoodSemantics.hasNegation(canonical)) return false;
        // If 2+ distinct non-overlapping product types were explicitly named, verify whether query is an item list
        String cleaned = (accented ? canonical : folded)
                .replaceAll("\\b(và|hoặc|với|cùng|mua|cần|tìm|cho|tôi|các|loại|tươi|sạch)\\b", " ")
                .replaceAll(" +", " ").trim();
        if (cleaned.isBlank()) return false;
        return requestedTypes.size() >= 2 && !contains(folded, "lau") && !contains(folded, "canh");
    }

    private static String detectCategoryScope(String canonical, String folded) {
        if (contains(canonical, "trái cây") || contains(canonical, "hoa quả")
                || contains(folded, "trai cay") || contains(folded, "hoa qua")) {
            return "trai cay";
        }
        if ((contains(canonical, "hải sản") || contains(canonical, "thủy sản")
                || contains(folded, "hai san") || contains(folded, "thuy san"))
                && !contains(folded, "lau") && !contains(folded, "nguyen lieu")) {
            return "hai san";
        }
        if (folded.startsWith("rau ") && (contains(folded, "lau") || contains(folded, "luoc") || contains(folded, "canh"))) {
            return "rau cu";
        }
        return null;
    }

    private static boolean nameContains(String name, String alias) {
        // Keep Vietnamese accents here: “cá” (fish) and “cà” (tomato/coffee) are different types.
        return contains(canonical(name), canonical(alias));
    }

    private static String canonical(String text) {
        return Normalizer.normalize(Objects.toString(text, ""), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private static boolean contains(String text, String phrase) {
        if (text == null || phrase == null || text.isBlank() || phrase.isBlank()) return false;
        return (" " + text + " ").contains(" " + phrase + " ");
    }

    private static List<List<String>> loadTypes() {
        try (var input = new ClassPathResource("search-product-types.json").getInputStream()) {
            return new ObjectMapper().readValue(input, new TypeReference<>() {});
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load product search vocabulary", error);
        }
    }
}
