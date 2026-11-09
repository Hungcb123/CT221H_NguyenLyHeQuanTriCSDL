package com.ctu.freshgrocery;

import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.service.impl.ProductBm25;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProductBm25Test {
    Product product(int id, String name) {
        return Product.builder().maSP(id).tenSP(name).build();
    }

    @Test
    void followsBm25FormulaAndDeduplicatesQueryTerms() {
        var products = List.of(product(1, "cá"), product(2, "bò"));
        var scores = ProductBm25.score(products, "CÁ cá");
        assertEquals(Math.log(2), scores.get(1), 0.00001);
        assertFalse(scores.containsKey(2));
    }

    @Test
    void lengthNormalizationPrefersConciseMatchingDocument() {
        var scores = ProductBm25.score(List.of(product(1, "cá"), product(2, "cá tươi sạch ngon")), "cá");
        assertTrue(scores.get(1) > scores.get(2));
    }

    @Test
    void documentFrequencyMakesRareTermsMoreInformative() {
        var scores = ProductBm25.score(List.of(product(1, "cá hồi"), product(2, "cá lóc"),
                product(3, "cá chép")), "cá hồi");
        assertTrue(scores.get(1) > scores.get(2));
    }

    @Test
    void handlesAccentsPunctuationAndEmptyCorpus() {
        assertEquals("ca loc", ProductBm25.normalize(" CÁ--LÓC! "));
        assertTrue(ProductBm25.score(List.of(), "cá").isEmpty());
        assertTrue(ProductBm25.score(List.of(product(1, "cá")), "!!!").isEmpty());
        assertFalse(ProductBm25.score(List.of(product(1, "cam")), "cá").containsKey(1));
    }
}
