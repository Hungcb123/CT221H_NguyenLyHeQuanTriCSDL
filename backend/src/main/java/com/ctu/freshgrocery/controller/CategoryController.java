package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.model.Category;
import com.ctu.freshgrocery.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "3. Danh mục Thực phẩm (Category Module)", description = "Tra cứu các nhóm hàng thực phẩm tươi sống")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả danh mục thực phẩm tươi sống")
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách danh mục thành công", categories));
    }
}
