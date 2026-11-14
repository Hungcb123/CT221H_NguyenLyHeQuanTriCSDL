package com.ctu.freshgrocery;

import com.ctu.freshgrocery.dto.SemanticProductResponse;
import com.ctu.freshgrocery.service.impl.ProductQueryIntent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CatalogAliasTest {
    SemanticProductResponse product(String name) {
        return SemanticProductResponse.builder().tenSP(name).build();
    }
    @Test void corianderDoesNotIncludeCulantro() {
        var intent = new ProductQueryIntent("rau mùi");
        assertTrue(intent.permits(product("Ngò rí tươi")));
        assertFalse(intent.permits(product("Ngò gai tươi")));
    }
    @Test void culantroDoesNotIncludeCoriander() {
        var intent = new ProductQueryIntent("rau mùi tàu");
        assertTrue(intent.permits(product("Ngò gai tươi")));
        assertFalse(intent.permits(product("Ngò rí tươi")));
    }
    @Test void SouthernJicamaNameDoesNotMeanCassava() {
        var intent = new ProductQueryIntent("củ sắn");
        assertTrue(intent.permits(product("Củ đậu tươi")));
        assertFalse(intent.permits(product("Khoai mì tươi")));
    }
    @Test void chickenBreastDoesNotMeanBeefBreast() {
        var intent = new ProductQueryIntent("lườn gà");
        assertTrue(intent.permits(product("Ức gà phi lê")));
        assertFalse(intent.permits(product("Thịt bò ức")));
    }
    @Test void pineappleAliasDoesNotHijackHerbOrRiceQueries() {
        assertTrue(new ProductQueryIntent("rau thơm").permits(product("Ngò rí tươi")));
        assertTrue(new ProductQueryIntent("gạo thơm").permits(product("Gạo thơm ST25 túi 5kg")));
    }
}
