package com.ctu.freshgrocery.admin;

import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.service.ProductTypeService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductLookupTest {
    @Mock ProductRepository products;
    @Mock ProductTypeService productTypes;
    @InjectMocks AdminController controller;

    private Product item(int id, String name, boolean deleted, Integer parent) {
        return Product.builder().maSP(id).tenSP(name).moTa("Nấu cùng thịt heo")
                .trangThaiXoa(deleted).maSPCha(parent).build();
    }
    @SuppressWarnings("unchecked")
    @Test void paginationCountsOnlyNameMatchesAndKeepsRootAndVisibilityFilters() {
        when(products.findAll()).thenReturn(List.of(item(2,"Thịt heo",false,null),
                item(3,"Thịt heo xay",false,null),item(4,"Thịt heo ẩn",true,null),
                item(5,"Thịt heo - 300g",false,2),item(1202,"Tiêu đen",false,null)));
        when(productTypes.metadata(anyInt())).thenReturn(Map.of());
        when(productTypes.stockByParent(anyList())).thenReturn(Map.of());
        var data=(Map<String,Object>)controller.products("thit heo",false,2,1).getData();
        assertEquals(2,data.get("totalElements"));assertEquals(2,data.get("totalPages"));
        var items=(List<Map<String,Object>>)data.get("items");
        assertEquals(List.of(3),items.stream().map(r->r.get("maSP")).toList());
    }
    @SuppressWarnings("unchecked")
    @Test void exactCodeAlsoWorksInDeletedCatalog() {
        when(products.findAll()).thenReturn(List.of(item(1204,"TEST_COD",true,null),item(2,"Thịt heo",false,null)));
        when(productTypes.metadata(anyInt())).thenReturn(Map.of());
        when(productTypes.stockByParent(anyList())).thenReturn(Map.of());
        var data=(Map<String,Object>)controller.products(" #1204 ",true,1,20).getData();
        assertEquals(1,data.get("totalElements"));
        var items=(List<Map<String,Object>>)data.get("items");assertEquals(1204,items.getFirst().get("maSP"));
    }
}
