package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import java.util.*;

/** Domain vocabulary for named product requests, not a list of canned query answers. */
public final class ProductQueryIntent {
    private static final List<List<String>> TYPES = loadTypes();
    private final List<String> requestedType;
    private final boolean cookingIntent;

    public ProductQueryIntent(String query) {
        String text = ProductBm25.normalize(query);
        cookingIntent = List.of("nguyen lieu", "nau", "lam", "mon", "de", "goi y", "bua", "thich hop")
                .stream().anyMatch(term -> contains(text, term));
        String canonical = canonical(query);
        boolean accentedQuery = query.codePoints().anyMatch(c -> c > 127);
        java.util.function.Predicate<String> mentioned = alias -> accentedQuery
                ? contains(canonical, canonical(alias)) : contains(text, ProductBm25.normalize(alias));
        requestedType = TYPES.stream().filter(aliases -> aliases.stream().anyMatch(mentioned))
                .max(Comparator.comparingInt(aliases -> aliases.stream().filter(mentioned)
                        .mapToInt(String::length).max().orElse(0))).orElse(List.of());
    }

    public boolean allowsSemanticOnly() { return cookingIntent; }

    public boolean permits(SemanticProductResponse product) {
        if (requestedType.isEmpty()) return true;
        return requestedType.stream().anyMatch(alias -> nameContains(product.getTenSP(), alias));
    }

    public boolean requestedTypeIsMissing(List<Product> catalog) {
        return !requestedType.isEmpty() && catalog.stream().noneMatch(product -> requestedType.stream()
                .anyMatch(alias -> nameContains(product.getTenSP(), alias)));
    }

    private static boolean nameContains(String name, String alias) {
        // Keep Vietnamese accents here: “cá” (fish) and “cà” (tomato/coffee) are different types.
        return contains(canonical(name), canonical(alias));
    }

    private static String canonical(String text) {
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private static boolean contains(String text, String phrase) {
        return (" " + text + " ").contains(" " + phrase + " ");
    }

    private static List<List<String>> loadTypes() {
        try (var input = new ClassPathResource("search-product-types.json").getInputStream()) {
            List<List<String>> types = new ObjectMapper().readValue(input, new TypeReference<>() {});
            return types;
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load product search vocabulary", error);
        }
    }
}
