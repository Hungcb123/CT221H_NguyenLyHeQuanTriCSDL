package com.ctu.freshgrocery.admin;

import com.ctu.freshgrocery.model.Product;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Catalog lookup by ID or name; descriptive/semantic matches belong to storefront search. */
final class AdminProductSearch {
    private AdminProductSearch() {}

    static List<Product> find(List<Product> catalog, String query) {
        String input = query.strip();
        Comparator<Product> newest = Comparator.comparing(Product::getMaSP).reversed();
        if (input.isEmpty()) return catalog.stream().sorted(newest).toList();
        String id = input.replaceFirst("^#\\s*", "");
        if (id.matches("[0-9]+")) {
            return catalog.stream().filter(p -> p.getMaSP().toString().equals(id)).toList();
        }
        String nameQuery = normalize(input);
        if (nameQuery.isEmpty()) return List.of();
        var terms = nameQuery.split(" ");
        return catalog.stream().filter(p -> {
            var words = normalize(p.getTenSP()).split(" ");
            return Arrays.stream(terms).allMatch(term -> Arrays.stream(words).anyMatch(word -> word.startsWith(term)));
        }).sorted(Comparator.<Product>comparingInt(p -> rank(normalize(p.getTenSP()), nameQuery))
                .thenComparing(newest)).toList();
    }

    private static int rank(String name, String query) {
        if (name.equals(query)) return 0;
        if (name.startsWith(query)) return 1;
        if (name.contains(query)) return 2;
        return 3;
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}
