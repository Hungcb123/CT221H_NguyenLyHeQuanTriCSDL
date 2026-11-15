package com.ctu.freshgrocery.admin;

import com.ctu.freshgrocery.model.Product;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminProductSearchTest {
    private Product item(int id, String name, String description) {
        return Product.builder().maSP(id).tenSP(name).moTa(description).build();
    }
    private List<Integer> ids(List<Product> catalog, String query) {
        return AdminProductSearch.find(catalog, query).stream().map(Product::getMaSP).toList();
    }

    @Test void excludesDescriptionOnlyMatchesButKeepsNamesContainingMeat() {
        var catalog = List.of(item(1202,"Tiêu đen", "Ướp thịt heo"), item(1195,"Hành tím", "Kho thịt"),
                item(2,"Thịt ba rọi heo VietGAP", ""), item(1201,"Hạt nêm thịt thăn 400g", ""));
        assertEquals(List.of(2,1201), ids(catalog,"thịt"));
    }
    @Test void requiresEveryQueryWordInNameRegardlessOfOrder() {
        var catalog = List.of(item(2,"Thịt ba rọi heo VietGAP", ""), item(3,"Thịt bò", "Ba rọi heo"),
                item(4,"Thịt heo xay", ""));
        assertEquals(List.of(2),ids(catalog,"heo thịt rọi ba"));
        assertTrue(ids(catalog,"thịt ba rọi gà").isEmpty());
    }
    @Test void ignoresAccentsCaseExtraSpacesAndUnicodeComposition() {
        var catalog = List.of(item(2,"Thịt ba rọi heo VietGAP", ""),item(5,"Đậu hũ", ""));
        assertEquals(List.of(2),ids(catalog,"  THIT   BA ROI  "));
        assertEquals(List.of(2),ids(catalog,"thi\u0323t ba ro\u0323i"));
        assertEquals(List.of(5),ids(catalog,"DAU HU"));
    }
    @Test void ranksExactThenPrefixThenPhraseThenReorderedWords() {
        var catalog = List.of(item(100,"Kho heo thịt", ""),item(50,"Hộp thịt heo", ""),
                item(20,"Thịt heo xay", ""),item(1,"Thịt heo", ""));
        assertEquals(List.of(1,20,50,100),ids(catalog,"thịt heo"));
    }
    @Test void looksUpExactIdWithOptionalHashAndTrimming() {
        var catalog = List.of(item(2,"Thịt heo", ""),item(12,"Gói 2 kg", ""));
        assertEquals(List.of(2),ids(catalog,"  #2  "));
        assertEquals(List.of(2),ids(catalog,"2"));
        assertTrue(ids(catalog,"99999").isEmpty());
        assertTrue(ids(catalog,"99999999999999999999").isEmpty());
    }
    @Test void supportsTypingPrefixesWithoutMatchingInsideUnrelatedWords() {
        var catalog = List.of(item(2,"Thịt ba rọi heo", ""),item(4,"Hạt tiêu", ""));
        assertEquals(List.of(2),ids(catalog,"thit ba ro"));
        assertTrue(ids(catalog,"hat ieu").isEmpty());
    }
    @Test void blankKeepsNewestFirstAndPunctuationOnlyDoesNotReturnEverything() {
        var catalog = List.of(item(2,"Thịt heo", ""),item(12,"Cà rốt", ""));
        assertEquals(List.of(12,2),ids(catalog,"  "));
        assertTrue(ids(catalog,"!!!").isEmpty());
    }
}
