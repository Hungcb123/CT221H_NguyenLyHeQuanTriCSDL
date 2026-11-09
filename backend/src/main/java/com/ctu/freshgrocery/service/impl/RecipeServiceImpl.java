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
    private final com.ctu.freshgrocery.repository.ProductRepository productRepository;
    private final com.ctu.freshgrocery.service.CartService cartService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

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
        var terms = queryTerms(keyword);
        return recipeRepository.findAll().stream()
                .filter(r -> terms.isEmpty() || relevance(r.getTenMonAn(), terms) > 0)
                .map(r -> buildRecipeDetail(r, servings))
                .sorted(java.util.Comparator.<RecipeDetailDto>comparingInt(r -> relevance(r.getTenMonAn(), terms)).reversed()
                        .thenComparing(r -> !Boolean.TRUE.equals(r.getDuNguyenLieuTrongKho()))
                        .thenComparing(RecipeDetailDto::getMaCongThuc))
                .findFirst().orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Chưa có công thức phù hợp với yêu cầu này trong cửa hàng."));
    }

    @Override
    @Transactional
    public Map<String, Object> addRecipeToCart(Integer customerId, Integer recipeId, Integer servings) {
        if (!customerRepository.existsById(customerId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng với ID: " + customerId);
        }
        if (!recipeRepository.existsById(recipeId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy công thức món ăn với ID: " + recipeId);
        }

        // Lock the customer's cart and candidate stock until the whole recipe is added.
        jdbc.queryForList("SELECT MaKH FROM KhachHang WITH (UPDLOCK,HOLDLOCK) WHERE MaKH=?", customerId);
        jdbc.queryForList("SELECT MaSP FROM SanPham WITH (UPDLOCK,HOLDLOCK)");
        var recipe = getRecipeDetail(recipeId, servings);
        if (!Boolean.TRUE.equals(recipe.getDuNguyenLieuTrongKho()))
            throw new AppException(HttpStatus.BAD_REQUEST, "Kho hiện không đủ nguyên liệu còn hạn cho món này. Vui lòng chọn món gợi ý khác.");
        for (var item : recipe.getNguyenLieuList()) {
            cartService.addToCart(com.ctu.freshgrocery.dto.AddToCartRequest.builder()
                    .maKH(customerId).maSP(item.getMaSP()).soLuong(item.getSoLuongDinhLuong().intValueExact()).build());
        }
        return Map.of("StatusCode", 1, "SoLuongMonDaThem", recipe.getNguyenLieuList().size(),
                "TongTienNguyenLieu", recipe.getTongTienUocTinh());
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
        var catalog = productRepository.findAll();
        var used = new java.util.HashMap<Integer,Integer>();

        for (RecipeIngredient ri : recipe.getIngredients()) {
            Product p = ri.getProduct();
            if (p != null) {
                BigDecimal scaledQty = ri.getSoLuongDinhLuong()
                        .multiply(BigDecimal.valueOf(scaleRatio))
                        .setScale(2, RoundingMode.CEILING);

                int roundedQty = (int) Math.ceil(scaledQty.doubleValue());
                Product original = p;
                // Resolve a sellable SKU for the ingredient instead of buying the template's fixed ID.
                p = catalog.stream().filter(candidate -> sameIngredient(original, candidate))
                        .filter(candidate -> available(candidate, roundedQty + used.getOrDefault(candidate.getMaSP(), 0)))
                        .sorted(java.util.Comparator.<Product,Boolean>comparing(candidate -> !candidate.getMaSP().equals(original.getMaSP()))
                                .thenComparing(Product::getHanSuDung).thenComparing(Product::getMaSP))
                        .findFirst().orElse(original);
                used.merge(p.getMaSP(), roundedQty, Integer::sum);
                BigDecimal lineCost = p.getGia().multiply(BigDecimal.valueOf(roundedQty));
                totalCost = totalCost.add(lineCost);

                String unavailable = Boolean.TRUE.equals(p.getTrangThaiXoa()) ? "Ngừng kinh doanh"
                        : p.getHanSuDung() == null || !p.getHanSuDung().isAfter(java.time.LocalDateTime.now()) ? "Đã hết hạn"
                        : p.getSoLuongTon() == null || p.getSoLuongTon() < roundedQty ? "Không đủ số lượng" : null;
                boolean inStock = unavailable == null;
                if (!inStock) allAvailable = false;

                items.add(RecipeDetailDto.IngredientItemDto.builder()
                        .maSP(p.getMaSP())
                        .tenSP(p.getTenSP())
                        .soLuongDinhLuong(BigDecimal.valueOf(roundedQty))
                        .donViTinh(p.getDonViTinh())
                        .donGia(p.getGia())
                        .thanhTien(lineCost)
                        .tonKhoHienTai(p.getSoLuongTon())
                        .conHang(inStock)
                        .lyDoKhongKhaDung(unavailable)
                        .thayTheCho(p.getMaSP().equals(original.getMaSP()) ? null : original.getTenSP())
                        .build());
            } else {
                allAvailable = false;
            }
        }

        return RecipeDetailDto.builder()
                .maCongThuc(recipe.getMaCongThuc())
                .tenMonAn(recipe.getTenMonAn())
                .moTa(recipe.getMoTa())
                .khauPhan(targetServings)
                .huongDanNau(recipe.getHuongDanNau())
                .tongTienUocTinh(totalCost)
                .duNguyenLieuTrongKho(allAvailable && !items.isEmpty())
                .nguyenLieuList(items)
                .build();
    }

    private boolean available(Product p, int quantity) {
        return !Boolean.TRUE.equals(p.getTrangThaiXoa()) && p.getHanSuDung() != null
                && p.getHanSuDung().isAfter(java.time.LocalDateTime.now()) && p.getSoLuongTon() != null && p.getSoLuongTon() >= quantity;
    }
    private boolean sameIngredient(Product original, Product candidate) {
        if (original.getMaSP().equals(candidate.getMaSP())) return true;
        if (!java.util.Objects.equals(original.getMaDanhMuc(), candidate.getMaDanhMuc())
                || !java.util.Objects.equals(original.getDonViTinh(), candidate.getDonViTinh())) return false;
        var name = ProductBm25.normalize(original.getTenSP()).split(" +");
        if (name.length < 2) return false;
        String anchor = name[0] + " " + name[1];
        return (" " + ProductBm25.normalize(candidate.getTenSP()) + " ").contains(" " + anchor + " ");
    }
    private java.util.Set<String> queryTerms(String text) {
        var ignored = java.util.Set.of("toi","muon","nau","mon","gi","goi","y","hom","nay","cho","nguoi","phan","khau","nguyen","lieu","con","co","san","trong","kho","an","bua","voi","hay","lam");
        return java.util.Arrays.stream(ProductBm25.normalize(text).split(" +"))
                .filter(t -> !t.isBlank() && !t.matches("\\d+") && !ignored.contains(t))
                .collect(java.util.stream.Collectors.toSet());
    }
    private int relevance(String title, java.util.Set<String> terms) {
        var words = new java.util.HashSet<>(java.util.Arrays.asList(ProductBm25.normalize(title).split(" +")));
        return (int) terms.stream().filter(words::contains).count();
    }
}
