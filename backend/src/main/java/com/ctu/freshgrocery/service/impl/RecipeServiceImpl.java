package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.RecipeDetailDto;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.model.Product;
import com.ctu.freshgrocery.model.Recipe;
import com.ctu.freshgrocery.model.RecipeIngredient;
import com.ctu.freshgrocery.repository.CustomerRepository;
import com.ctu.freshgrocery.repository.RecipeRepository;
import com.ctu.freshgrocery.repository.RecipeSpRepository;
import com.ctu.freshgrocery.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeSpRepository recipeSpRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public RecipeDetailDto getRecipeDetail(Integer recipeId, Integer servings) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy công thức món ăn với ID: " + recipeId));

        return buildRecipeDetail(recipe, servings);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeDetailDto searchRecipeWithIngredients(String keyword, Integer servings) {
        List<Recipe> matches = recipeRepository.findByTenMonAnContainingIgnoreCase(keyword);
        if (matches.isEmpty()) {
            // Thử tìm theo từ khóa không dấu hoặc từ khóa chính
            matches = recipeRepository.findAll().stream()
                    .filter(r -> r.getTenMonAn().toLowerCase().contains(keyword.toLowerCase())
                            || (r.getMoTa() != null && r.getMoTa().toLowerCase().contains(keyword.toLowerCase())))
                    .collect(Collectors.toList());
        }

        if (matches.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy công thức món ăn phù hợp với yêu cầu: " + keyword);
        }

        return buildRecipeDetail(matches.get(0), servings);
    }

    @Override
    public Map<String, Object> addRecipeToCart(Integer customerId, Integer recipeId, Integer servings) {
        if (!customerRepository.existsById(customerId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng với ID: " + customerId);
        }
        if (!recipeRepository.existsById(recipeId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy công thức món ăn với ID: " + recipeId);
        }

        Map<String, Object> result = recipeSpRepository.executeThemNguyenLieuVaoGioHang(customerId, recipeId, servings);
        Integer statusCode = (Integer) result.get("StatusCode");
        String message = (String) result.get("Message");

        if (statusCode == null || statusCode < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, message != null ? message : "Lỗi thêm nguyên liệu vào giỏ hàng");
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeDetailDto> getAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(r -> buildRecipeDetail(r, r.getKhauPhanMacDinh()))
                .collect(Collectors.toList());
    }

    private RecipeDetailDto buildRecipeDetail(Recipe recipe, Integer servings) {
        int targetServings = (servings != null && servings > 0) ? servings : recipe.getKhauPhanMacDinh();
        double scaleRatio = (double) targetServings / recipe.getKhauPhanMacDinh();

        List<RecipeDetailDto.IngredientItemDto> items = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        boolean allAvailable = true;

        for (RecipeIngredient ri : recipe.getIngredients()) {
            Product p = ri.getProduct();
            if (p != null) {
                BigDecimal scaledQty = ri.getSoLuongDinhLuong()
                        .multiply(BigDecimal.valueOf(scaleRatio))
                        .setScale(2, RoundingMode.CEILING);

                int roundedQty = (int) Math.ceil(scaledQty.doubleValue());
                BigDecimal lineCost = p.getGia().multiply(BigDecimal.valueOf(roundedQty));
                totalCost = totalCost.add(lineCost);

                boolean inStock = p.getSoLuongTon() >= roundedQty;
                if (!inStock) allAvailable = false;

                items.add(RecipeDetailDto.IngredientItemDto.builder()
                        .maSP(p.getMaSP())
                        .tenSP(p.getTenSP())
                        .soLuongDinhLuong(scaledQty)
                        .donViTinh(ri.getDonViTinh())
                        .donGia(p.getGia())
                        .thanhTien(lineCost)
                        .tonKhoHienTai(p.getSoLuongTon())
                        .conHang(inStock)
                        .build());
            }
        }

        return RecipeDetailDto.builder()
                .maCongThuc(recipe.getMaCongThuc())
                .tenMonAn(recipe.getTenMonAn())
                .moTa(recipe.getMoTa())
                .khauPhan(targetServings)
                .huongDanNau(recipe.getHuongDanNau())
                .tongTienUocTinh(totalCost)
                .duNguyenLieuTrongKho(allAvailable)
                .nguyenLieuList(items)
                .build();
    }
}
