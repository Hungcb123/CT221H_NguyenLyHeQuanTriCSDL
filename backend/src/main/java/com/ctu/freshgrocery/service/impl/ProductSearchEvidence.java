package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.SemanticProductResponse;
import java.text.Normalizer;
import java.util.*;

/** Conservative lexical evidence for primary results; semantic-only neighbors are suggestions. */
public final class ProductSearchEvidence {
    // Remove request wording BEFORE accent folding: “để” must not erase the product term “dê”.
    private static final Set<String> REQUEST_WORDS = Set.of(
            "để", "làm", "nấu", "món", "ăn", "kèm", "cho", "với", "cần", "tìm", "muốn", "giúp", "tôi");

    private ProductSearchEvidence() {}

    public static Set<String> requiredTerms(String query) {
        String text = Normalizer.normalize(Objects.toString(query, ""), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\bnguyên liệu\\b", " ").trim();
        Set<String> terms = new LinkedHashSet<>();
        for (String term : text.split(" +")) {
            if (!term.isBlank() && !REQUEST_WORDS.contains(term)) terms.add(term);
        }
        String normalized = ProductBm25.normalize(String.join(" ", terms));
        return normalized.isBlank() ? Set.of() : new LinkedHashSet<>(Arrays.asList(normalized.split(" +")));
    }

    public static boolean matches(Set<String> required, SemanticProductResponse product) {
        if (required.isEmpty()) return false;
        String document = ProductBm25.normalize(product.getTenSP() + " "
                + Objects.toString(product.getMoTa(), "") + " "
                + Objects.toString(product.getTenDanhMuc(), ""));
        Set<String> terms = new HashSet<>(Arrays.asList(document.split(" +")));
        return terms.containsAll(required);
    }
}
