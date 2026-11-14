package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.ctu.freshgrocery.model.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.core.io.ClassPathResource;

import java.text.Normalizer;
import java.util.*;

/**
 * Bilingual culinary & grocery semantic bridge for Vietnamese queries and products.
 * Aligns Vietnamese cooking, nutrition, and regional vocabulary with the ONNX embedding space
 * and provides conservative typo recovery without mutating known domain words.
 */
public final class VietnameseFoodSemantics {

    @Data
    public static class ConceptDef {
        private String id;
        private List<String> triggers = List.of();
        private String english = "";
        private List<String> productSignals = List.of();
    }

    @Data
    public static class KnowledgeFile {
        private List<ConceptDef> concepts = List.of();
        private Map<String, String> ingredientEnglish = Map.of();
    }

    private static final KnowledgeFile KNOWLEDGE = loadKnowledge();
    private static final List<List<String>> PRODUCT_TYPES = loadTypes();

    private static final Map<String, String> GENERIC_QUERY_HINTS = Map.ofEntries(
            Map.entry("rau", "fresh vegetables leafy greens"),
            Map.entry("trai cay", "fresh tropical fruits"),
            Map.entry("hoa qua", "fresh fruits"),
            Map.entry("hai san", "fresh seafood shrimp squid clam fish crab"),
            Map.entry("thuy san", "fresh fish and seafood"),
            Map.entry("thit", "fresh meat pork beef chicken"),
            Map.entry("gia vi", "cooking spices seasoning herbs"),
            Map.entry("canh", "soup broth"),
            Map.entry("lau", "hotpot"),
            Map.entry("nuong", "bbq grill"),
            Map.entry("hap", "steamed"),
            Map.entry("xao", "stir fry"),
            Map.entry("kho", "braised"),
            Map.entry("luoc", "boiled"),
            Map.entry("chao", "porridge"),
            Map.entry("salad", "fresh salad"),
            Map.entry("goi", "vietnamese salad")
    );

    private static final Set<String> COMMON_QUERY_WORDS = Collections.unmodifiableSet(new HashSet<>(List.of(
            "nguyen", "lieu", "de", "lam", "nau", "mon", "an", "kem", "cho", "voi", "can", "tim",
            "giup", "toi", "goi", "y", "bua", "sang", "trua", "chieu", "nhanh", "gon", "giau",
            "protein", "dam", "vitamin", "detox", "thanh", "loc", "giam", "mo", "tap", "gym",
            "eat", "clean", "kieng", "thuc", "pham", "do", "tuoi", "ngon", "sach", "loai", "nao",
            "cac", "nhung", "gi", "hom", "nay", "mien", "tay", "khong", "duong", "it", "beo"
    )));

    private VietnameseFoodSemantics() {}

    public static boolean isInternalTestProduct(Product product) {
        if (product == null || product.getTenSP() == null) return false;
        return product.getTenSP().trim().toUpperCase(Locale.ROOT).startsWith("[TEST]");
    }

    public static boolean queryRequestsTestProduct(String query) {
        String norm = ProductBm25.normalize(query);
        return containsPhrase(norm, "test") || containsPhrase(norm, "goi thu");
    }

    public static boolean hasNegation(String query) {
        String canonical = canonical(query);
        String folded = ProductBm25.normalize(query);
        return containsPhrase(canonical, "không") || containsPhrase(canonical, "trừ")
                || containsPhrase(folded, "khong");
    }

    /**
     * Repairs obvious single-token typos (repeated letters like "ca hoii", "tom suu", "ca looc",
     * or edit-distance-1 token inside a multi-word product name like "rau muon" -> "rau muong")
     * while NEVER altering tokens that already exist in the catalog or domain vocabulary.
     */
    public static String resolveTypoQuery(String query, List<Product> catalog) {
        if (query == null || query.isBlank() || catalog == null || catalog.isEmpty() || hasNegation(query)) {
            return query;
        }
        String normalized = ProductBm25.normalize(query);
        if (normalized.isBlank()) return query;
        String[] qTokens = normalized.split(" +");

        Set<String> knownVocab = new HashSet<>(COMMON_QUERY_WORDS);
        List<Set<String>> productNameTokens = new ArrayList<>();
        for (Product p : catalog) {
            String nameNorm = ProductBm25.normalize(p.getTenSP());
            if (!nameNorm.isEmpty()) {
                Set<String> nameSet = new LinkedHashSet<>(Arrays.asList(nameNorm.split(" +")));
                productNameTokens.add(nameSet);
                knownVocab.addAll(nameSet);
            }
            String descNorm = ProductBm25.normalize(p.getMoTa());
            if (!descNorm.isEmpty()) {
                knownVocab.addAll(Arrays.asList(descNorm.split(" +")));
            }
        }
        for (List<String> group : PRODUCT_TYPES) {
            for (String alias : group) {
                String aliasNorm = ProductBm25.normalize(alias);
                if (!aliasNorm.isEmpty()) knownVocab.addAll(Arrays.asList(aliasNorm.split(" +")));
            }
        }

        boolean changed = false;
        String[] repaired = Arrays.copyOf(qTokens, qTokens.length);
        for (int i = 0; i < qTokens.length; i++) {
            String tok = qTokens[i];
            if (tok.length() < 3 || knownVocab.contains(tok)) continue;

            // 1. Collapse repeated consecutive letters (e.g. "hoii" -> "hoi", "looc" -> "loc", "suu" -> "su")
            String collapsed = tok.replaceAll("(.)\\1+", "$1");
            if (!collapsed.equals(tok) && collapsed.length() >= 2 && knownVocab.contains(collapsed)) {
                repaired[i] = collapsed;
                changed = true;
                continue;
            }

            // 2. Contextual edit-distance-1 recovery anchored by a co-occurring product-name token
            if (qTokens.length >= 2 && tok.length() >= 4) {
                Set<String> matches = new LinkedHashSet<>();
                for (Set<String> nameSet : productNameTokens) {
                    boolean hasAnchor = false;
                    for (int j = 0; j < qTokens.length; j++) {
                        if (i != j && nameSet.contains(repaired[j])) {
                            hasAnchor = true;
                            break;
                        }
                    }
                    if (!hasAnchor) continue;
                    for (String candidate : nameSet) {
                        if (candidate.length() >= 4 && isDistanceOne(tok, candidate)) {
                            matches.add(candidate);
                        }
                    }
                }
                if (matches.size() == 1) {
                    repaired[i] = matches.iterator().next();
                    changed = true;
                }
            }
        }
        return changed ? String.join(" ", repaired) : query;
    }

    /**
     * Builds a rich bilingual semantic document for ONNX embedding indexing in Qdrant.
     * Places concise English semantic signals first so they are never truncated.
     */
    public static String buildSemanticDocument(Product product, String categoryName, List<String> recipeNames) {
        String name = Objects.toString(product.getTenSP(), "");
        String desc = Objects.toString(product.getMoTa(), "");
        String cat = Objects.toString(categoryName, "");
        String recipes = recipeNames == null || recipeNames.isEmpty() ? "" : String.join(", ", recipeNames);

        String combinedCanonical = canonical(name + " " + desc + " " + recipes);
        String combinedFolded = ProductBm25.normalize(name + " " + desc + " " + recipes);

        StringBuilder englishSignals = new StringBuilder();
        for (var entry : KNOWLEDGE.getIngredientEnglish().entrySet()) {
            if (containsPhrase(combinedCanonical, canonical(entry.getKey()))
                    || containsPhrase(combinedFolded, ProductBm25.normalize(entry.getKey()))) {
                englishSignals.append(entry.getValue()).append(". ");
            }
        }
        for (ConceptDef concept : KNOWLEDGE.getConcepts()) {
            boolean matchesSignal = concept.getProductSignals().stream()
                    .anyMatch(sig -> containsPhrase(combinedCanonical, canonical(sig)));
            if (matchesSignal) {
                englishSignals.append(concept.getEnglish()).append(". ");
            }
        }

        StringBuilder sb = new StringBuilder();
        if (!englishSignals.isEmpty()) {
            sb.append(englishSignals);
        }
        sb.append(name).append(". ");
        if (!recipes.isBlank()) {
            sb.append("Mon an: ").append(recipes).append(". ");
        }
        if (!cat.isBlank()) {
            sb.append("Danh muc: ").append(cat).append(". ");
        }
        if (!desc.isBlank()) {
            sb.append(desc);
        }
        return sb.toString().trim();
    }

    /**
     * Expands a Vietnamese/English search query into the same bilingual embedding space
     * used when indexing products in Qdrant.
     */
    public static String buildQueryEmbeddingText(String query) {
        if (query == null || query.isBlank()) return "";
        String canonicalQuery = canonical(query);
        String foldedQuery = ProductBm25.normalize(query);
        boolean accented = query.codePoints().anyMatch(c -> c > 127);

        StringBuilder expanded = new StringBuilder();
        for (ConceptDef concept : matchedConcepts(query)) {
            expanded.append(concept.getEnglish()).append(". ");
        }
        for (var entry : KNOWLEDGE.getIngredientEnglish().entrySet()) {
            String key = entry.getKey();
            boolean hit = accented
                    ? containsPhrase(canonicalQuery, canonical(key))
                    : containsPhrase(foldedQuery, ProductBm25.normalize(key));
            if (hit) {
                expanded.append(entry.getValue()).append(". ");
            }
        }
        for (var hint : GENERIC_QUERY_HINTS.entrySet()) {
            if (containsPhrase(foldedQuery, hint.getKey())) {
                expanded.append(hint.getValue()).append(" ");
            }
        }
        expanded.append(query);
        return expanded.toString().trim();
    }

    /**
     * Checks whether a product is grounded in a matched SQL recipe or verified culinary/nutritional concept.
     * Requires non-blank product descriptions for concept matching so bare mocked unit-test items stay conservative.
     */
    public static boolean matchesCulinaryOrRecipeConcept(
            String query, SemanticProductResponse product, Set<Integer> recipeMatchedProductIds) {
        if (query == null || product == null || hasNegation(query)) return false;

        if (recipeMatchedProductIds != null && product.getMaSP() != null
                && recipeMatchedProductIds.contains(product.getMaSP())) {
            return true;
        }

        if (product.getMoTa() == null || product.getMoTa().isBlank()) {
            return false;
        }

        List<ConceptDef> concepts = matchedConcepts(query);
        if (concepts.isEmpty()) return false;

        String docCanonical = canonical(product.getTenSP() + " " + product.getMoTa());
        String docFolded = ProductBm25.normalize(product.getTenSP() + " " + product.getMoTa());

        // Every triggered concept must be supported by explicit signals in the product's name/description.
        for (ConceptDef concept : concepts) {
            boolean supported = concept.getProductSignals().stream().anyMatch(sig ->
                    containsPhrase(docCanonical, canonical(sig))
                            || containsPhrase(docFolded, ProductBm25.normalize(sig)));
            if (!supported) return false;
        }
        return true;
    }

    public static List<ConceptDef> matchedConcepts(String query) {
        if (query == null || query.isBlank()) return List.of();
        String canonicalQuery = canonical(query);
        String foldedQuery = ProductBm25.normalize(query);
        boolean accented = query.codePoints().anyMatch(c -> c > 127);

        List<ConceptDef> matched = new ArrayList<>();
        for (ConceptDef concept : KNOWLEDGE.getConcepts()) {
            boolean hit = concept.getTriggers().stream().anyMatch(trigger -> accented
                    ? containsPhrase(canonicalQuery, canonical(trigger))
                    : containsPhrase(foldedQuery, ProductBm25.normalize(trigger)));
            if (hit) matched.add(concept);
        }
        return matched;
    }

    private static boolean isDistanceOne(String a, String b) {
        int la = a.length();
        int lb = b.length();
        if (Math.abs(la - lb) > 1) return false;
        int i = 0, j = 0, edits = 0;
        while (i < la && j < lb) {
            if (a.charAt(i) == b.charAt(j)) {
                i++;
                j++;
            } else {
                if (++edits > 1) return false;
                if (la > lb) i++;
                else if (lb > la) j++;
                else { i++; j++; }
            }
        }
        if (i < la || j < lb) edits++;
        return edits == 1;
    }

    static String canonical(String text) {
        return Normalizer.normalize(Objects.toString(text, ""), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    static boolean containsPhrase(String text, String phrase) {
        if (text == null || phrase == null || text.isBlank() || phrase.isBlank()) return false;
        return (" " + text + " ").contains(" " + phrase + " ");
    }

    private static KnowledgeFile loadKnowledge() {
        try (var input = new ClassPathResource("search-culinary-knowledge.json").getInputStream()) {
            return new ObjectMapper().readValue(input, KnowledgeFile.class);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load search-culinary-knowledge.json", error);
        }
    }

    private static List<List<String>> loadTypes() {
        try (var input = new ClassPathResource("search-product-types.json").getInputStream()) {
            return new ObjectMapper().readValue(input,
                    new com.fasterxml.jackson.core.type.TypeReference<List<List<String>>>() {});
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load search-product-types.json", error);
        }
    }
}
