package com.ctu.freshgrocery;

import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.service.impl.ProductQueryIntent;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductQueryIntentTest {
    @Test
    void cookingPrepositionIsNotMistakenForGoatMeat() {
        var query = new ProductQueryIntent("thịt để kho tàu");
        assertFalse(query.requestedTypeIsMissing(List.of(Product.builder().tenSP("Thịt ba rọi heo").build())));
        assertTrue(query.permits(SemanticProductResponse.builder().tenSP("Thịt ba rọi heo").build()));
    }

    @Test
    void actualGoatRequestIsNotPork() {
        var query = new ProductQueryIntent("thịt dê");
        assertTrue(query.requestedTypeIsMissing(List.of(Product.builder().tenSP("Thịt ba rọi heo").build())));
    }

    @Test
    void fishIsNotTomatoDespiteSimilarFoldedSpelling() {
        assertFalse(new ProductQueryIntent("cá để nấu canh")
                .permits(SemanticProductResponse.builder().tenSP("Cà chua").build()));
    }

    @Test
    void unaccentedSpecificProductNameStillFindsItsType() {
        assertTrue(new ProductQueryIntent("ca hoi").permits(
                SemanticProductResponse.builder().tenSP("Cá hồi phi lê").build()));
        assertFalse(new ProductQueryIntent("ca hoi").permits(
                SemanticProductResponse.builder().tenSP("Cá lóc").build()));
    }

    @Test
    void chickenMeatRequestMustNotMatchEggs() {
        assertFalse(new ProductQueryIntent("thịt gà nguyên con").permits(
                SemanticProductResponse.builder().tenSP("Trứng gà ta").build()));
    }
}
