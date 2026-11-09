package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.model.Product;
import java.util.*;
import java.util.stream.Collectors;

/** Small-catalog BM25 over product name and description; rebuilt from the SQL snapshot. */
public final class ProductBm25 {
    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private ProductBm25() {}

    public static String normalize(String text) {
        // Canonicalize true regional synonyms before folding accents. Do not merge
        // product families/cuts (e.g. ba chỉ and nạc vai) from the intent vocabulary.
        String canonical = (text == null ? "" : java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC))
                .replaceAll("(?iu)\\blợn\\b", "heo")
                .replaceAll("(?iu)\\bthit lon\\b", "thit heo");
        return BertTokenizer.removeDiacritics(canonical)
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private static List<String> tokens(String text) {
        String normalized = normalize(text);
        return normalized.isEmpty() ? List.of() : Arrays.asList(normalized.split(" "));
    }

    public static Map<Integer, Float> score(List<Product> products, String query) {
        Set<String> queryTerms = new HashSet<>(tokens(query));
        if (products.isEmpty() || queryTerms.isEmpty()) return Map.of();
        Map<Integer, Map<String, Long>> frequencies = new HashMap<>();
        Map<Integer, Integer> lengths = new HashMap<>();
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (Product product : products) {
            List<String> document = tokens(product.getTenSP() + " "
                    + Objects.toString(product.getMoTa(), ""));
            Map<String, Long> counts = document.stream()
                    .collect(Collectors.groupingBy(term -> term, Collectors.counting()));
            frequencies.put(product.getMaSP(), counts);
            lengths.put(product.getMaSP(), document.size());
            counts.keySet().forEach(term -> documentFrequency.merge(term, 1, Integer::sum));
        }
        double averageLength = lengths.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        if (averageLength == 0) return Map.of();
        Map<Integer, Float> scores = new HashMap<>();
        for (Product product : products) {
            double score = 0;
            var counts = frequencies.get(product.getMaSP());
            double lengthRatio = lengths.get(product.getMaSP()) / averageLength;
            for (String term : queryTerms) {
                long tf = counts.getOrDefault(term, 0L);
                if (tf == 0) continue;
                int df = documentFrequency.getOrDefault(term, 0);
                double idf = Math.log1p((products.size() - df + 0.5) / (df + 0.5));
                score += idf * tf * (K1 + 1) / (tf + K1 * (1 - B + B * lengthRatio));
            }
            if (score > 0) scores.put(product.getMaSP(), (float) score);
        }
        return scores;
    }
}
