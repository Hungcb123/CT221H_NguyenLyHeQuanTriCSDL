package com.ctu.freshgrocery;

import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.ctu.freshgrocery.service.impl.ProductSearchEvidence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductSearchEvidenceTest {
    SemanticProductResponse product(String name, String description) {
        return SemanticProductResponse.builder().tenSP(name).moTa(description).build();
    }

    @Test
    void absentSquidMustNotTurnShrimpIntoAMatchingResult() {
        var terms = ProductSearchEvidence.requiredTerms("mực ống tươi");
        assertFalse(ProductSearchEvidence.matches(terms, product("Tôm sú tươi sống", "Hải sản")));
    }

    @Test
    void partialChickenOverlapIsNotAWholeChickenMatch() {
        var terms = ProductSearchEvidence.requiredTerms("thịt gà nguyên con");
        assertFalse(ProductSearchEvidence.matches(terms, product("Trứng gà ta", "Tươi sạch")));
        assertFalse(ProductSearchEvidence.matches(terms, product("Thịt ba rọi", "Thịt heo tươi")));
    }

    @Test
    void requestWordsDoNotBlockDescriptionSupportedIntent() {
        var terms = ProductSearchEvidence.requiredTerms("nguyên liệu để nấu món canh chua");
        assertTrue(ProductSearchEvidence.matches(terms, product("Cá lóc", "Cắt khúc nấu canh chua")));
    }

    @Test
    void goatIsNotRemovedAsARequestWordAndNegationIsPreserved() {
        var terms = ProductSearchEvidence.requiredTerms("thịt dê để nấu");
        assertTrue(terms.contains("de"));
        assertFalse(ProductSearchEvidence.matches(terms, product("Thịt heo", "Thịt tươi")));
        assertFalse(ProductSearchEvidence.matches(ProductSearchEvidence.requiredTerms("sữa không đường"),
                product("Sữa", "Sữa tươi")));
    }

    @Test
    void blankOrRequestOnlyQueryDoesNotConfirmEveryProduct() {
        assertFalse(ProductSearchEvidence.matches(ProductSearchEvidence.requiredTerms("nguyên liệu để nấu"),
                product("Cá lóc", "Cá tươi")));
    }
}
