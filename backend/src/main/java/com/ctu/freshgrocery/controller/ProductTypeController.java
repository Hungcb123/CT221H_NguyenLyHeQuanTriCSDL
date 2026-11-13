package com.ctu.freshgrocery.controller;
import com.ctu.freshgrocery.dto.*;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.repository.ProductSpRepository;
import com.ctu.freshgrocery.service.ProductTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequiredArgsConstructor
public class ProductTypeController {
    private final ProductTypeService types;
    private final ProductSpRepository products;

    @GetMapping("/api/v1/admin/products/{id}/types")
    public ApiResponse<List<ProductResponse>> adminTypes(@PathVariable int id) {
        var parent = products.callLayChiTietSanPham(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm."));
        if (parent.getMaSPCha() != null) throw new AppException(HttpStatus.BAD_REQUEST, "Hãy sửa các loại trong sản phẩm sở hữu chúng.");
        return ApiResponse.ok("Các loại của sản phẩm", load(types.childIds(id, true)));
    }

    @GetMapping("/api/v1/products/{id}/types")
    public ApiResponse<List<ProductResponse>> types(@PathVariable int id) {
        var selected = products.callLayChiTietSanPham(id).filter(p -> !Boolean.TRUE.equals(p.getTrangThaiXoa()))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm."));
        int parentId = selected.getMaSPCha() == null ? id : selected.getMaSPCha();
        var parent = products.callLayChiTietSanPham(parentId).filter(p -> !Boolean.TRUE.equals(p.getTrangThaiXoa()))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Sản phẩm đã ngừng kinh doanh."));
        var children = load(types.childIds(parentId, false));
        return ApiResponse.ok("Các loại sản phẩm", children.isEmpty() ? List.of(parent) : children);
    }
    private List<ProductResponse> load(List<Integer> ids) {
        return ids.stream().map(products::callLayChiTietSanPham).flatMap(Optional::stream).toList();
    }
}
