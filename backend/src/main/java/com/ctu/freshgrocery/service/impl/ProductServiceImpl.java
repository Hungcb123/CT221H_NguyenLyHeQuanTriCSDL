package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.ProductCreateRequest;
import com.ctu.freshgrocery.dto.ProductPageResponse;
import com.ctu.freshgrocery.dto.ProductResponse;
import com.ctu.freshgrocery.dto.ProductUpdateRequest;
import com.ctu.freshgrocery.repository.ProductRepository;
import com.ctu.freshgrocery.repository.ProductSpRepository;
import com.ctu.freshgrocery.service.ProductService;
import com.ctu.freshgrocery.service.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import com.ctu.freshgrocery.model.Product;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductSpRepository productSpRepository;
    private final ProductRepository productRepository;
    private final SemanticSearchService semanticSearchService;

    @Override
    public ApiResponse<ProductResponse> createProduct(ProductCreateRequest request) {
        log.info("Creating fresh grocery product via Stored Procedure: {}", request.getTenSP());

        Map<String, Object> out = productSpRepository.callThemSanPhamMoi(request);
        Integer statusCode = (Integer) out.get("StatusCode");
        String message = (String) out.get("Message");
        Integer newMaSP = (Integer) out.get("NewMaSP");

        if (statusCode == null || statusCode != 1) {
            log.warn("Stored Procedure sp_ThemSanPhamMoi returned error: code={}, message={}", statusCode, message);
            return ApiResponse.error(statusCode != null ? statusCode : -99, message);
        }

        // Lấy chi tiết sản phẩm vừa tạo
        Optional<ProductResponse> productOpt = productSpRepository.callLayChiTietSanPham(newMaSP);
        productOpt.ifPresent(p -> productRepository.findById(p.getMaSP()).ifPresent(semanticSearchService::syncProduct));

        return productOpt
                .map(prod -> ApiResponse.ok(1, message, prod))
                .orElseGet(() -> ApiResponse.ok(1, message, ProductResponse.builder().maSP(newMaSP).build()));
    }

    @Override
    public ApiResponse<ProductResponse> updateProduct(Integer id, ProductUpdateRequest request) {
        log.info("Updating fresh grocery product ID={} via Stored Procedure", id);

        Map<String, Object> out = productSpRepository.callCapNhatSanPham(id, request);
        Integer statusCode = (Integer) out.get("StatusCode");
        String message = (String) out.get("Message");

        if (statusCode == null || statusCode != 1) {
            log.warn("Stored Procedure sp_CapNhatSanPham returned error: code={}, message={}", statusCode, message);
            return ApiResponse.error(statusCode != null ? statusCode : -99, message);
        }

        // Lấy lại chi tiết sau khi cập nhật và cập nhật vector trong Qdrant
        Optional<ProductResponse> productOpt = productSpRepository.callLayChiTietSanPham(id);
        productOpt.ifPresent(p -> productRepository.findById(p.getMaSP()).ifPresent(semanticSearchService::syncProduct));

        return productOpt
                .map(prod -> ApiResponse.ok(1, message, prod))
                .orElseGet(() -> ApiResponse.ok(1, message, null));
    }

    @Override
    public ApiResponse<String> deleteProduct(Integer id) {
        log.info("Triggering delete for product ID={}", id);

        if (!productSpRepository.callXoaSanPham(id)) {
            return ApiResponse.error(-1, "Sản phẩm không tồn tại hoặc đã bị xóa mềm trước đó.");
        }

        semanticSearchService.removeProduct(id);
        return ApiResponse.ok(1, "Đã ẩn sản phẩm và ghi nhật ký xóa mềm.", "Soft-deleted successfully");
    }

    @Override
    @SuppressWarnings("unchecked")
    public ApiResponse<ProductPageResponse> getProducts(
            Integer categoryId, String keyword, BigDecimal minPrice, BigDecimal maxPrice,
            String sortBy, int page, int size) {

        log.debug("Fetching paged products: cat={}, kw={}, sort={}, page={}, size={}", categoryId, keyword, sortBy, page, size);

        if (keyword != null && !keyword.isBlank()) {
            return searchByBm25(categoryId, keyword, minPrice, maxPrice, sortBy, page, size);
        }

        Map<String, Object> out = productSpRepository.callLayDanhSachSanPhamPhanTrang(
                categoryId, keyword, minPrice, maxPrice, sortBy, page, size);

        Integer totalRecords = (Integer) out.getOrDefault("TongSoBanGhi", 0);
        Integer totalPages = (Integer) out.getOrDefault("TongSoTrang", 1);
        List<ProductResponse> items = (List<ProductResponse>) out.getOrDefault("items", Collections.emptyList());

        ProductPageResponse pageData = ProductPageResponse.builder()
                .items(items)
                .currentPage(page)
                .pageSize(size)
                .totalRecords(totalRecords != null ? totalRecords : 0)
                .totalPages(totalPages != null ? totalPages : 1)
                .hasNext(page < (totalPages != null ? totalPages : 1))
                .hasPrevious(page > 1)
                .build();

        return ApiResponse.ok("Lấy danh sách sản phẩm thành công", pageData);
    }

    private ApiResponse<ProductPageResponse> searchByBm25(Integer categoryId, String keyword,
            BigDecimal minPrice, BigDecimal maxPrice, String sortBy, int page, int size) {
        List<Product> catalog = productRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getTrangThaiXoa())).toList();
        Map<Integer, Float> scores = ProductBm25.score(catalog, keyword);
        String query = ProductBm25.normalize(keyword);
        var intent = new ProductQueryIntent(keyword);
        var terms = ProductSearchEvidence.requiredTerms(keyword);
        List<ProductResponse> matches = catalog.stream()
                .filter(p -> scores.containsKey(p.getMaSP()))
                .sorted(Comparator.<Product, Boolean>comparing(p ->
                        (" " + ProductBm25.normalize(p.getTenSP()) + " ").contains(" " + query + " ")).reversed()
                        .thenComparing(Comparator.<Product, Float>comparing(p -> scores.get(p.getMaSP())).reversed())
                        .thenComparing(Product::getMaSP))
                .map(p -> productSpRepository.callLayChiTietSanPham(p.getMaSP()))
                .flatMap(Optional::stream)
                .filter(p -> !Boolean.TRUE.equals(p.getTrangThaiXoa()))
                .filter(p -> categoryId == null || categoryId <= 0 || categoryId.equals(p.getMaDanhMuc()))
                .filter(p -> minPrice == null || p.getGiaHienTai().compareTo(minPrice) >= 0)
                .filter(p -> maxPrice == null || p.getGiaHienTai().compareTo(maxPrice) <= 0)
                .filter(p -> {
                    var evidence = com.ctu.freshgrocery.dto.SemanticProductResponse.builder()
                            .tenSP(p.getTenSP()).moTa(p.getMoTa()).tenDanhMuc(p.getTenDanhMuc()).build();
                    return intent.permits(evidence) && ProductSearchEvidence.matches(terms, evidence);
                }).collect(java.util.stream.Collectors.toList());
        if ("GIA_ASC".equals(sortBy)) matches.sort(Comparator.comparing(ProductResponse::getGiaHienTai));
        if ("GIA_DESC".equals(sortBy)) matches.sort(Comparator.comparing(ProductResponse::getGiaHienTai).reversed());
        if ("HSD_ASC".equals(sortBy)) matches.sort(Comparator.comparing(ProductResponse::getHanSuDung,
                Comparator.nullsLast(Comparator.naturalOrder())));
        if ("TON_DESC".equals(sortBy)) matches.sort(Comparator.comparing(ProductResponse::getSoLuongTon).reversed());
        int current = Math.max(1, page);
        int pageSize = Math.clamp(size, 1, 100);
        int totalPages = (matches.size() + pageSize - 1) / pageSize;
        int start = (int) Math.min((long) (current - 1) * pageSize, matches.size());
        return ApiResponse.ok("Tìm kiếm sản phẩm bằng BM25", ProductPageResponse.builder()
                .items(matches.subList(start, Math.min(start + pageSize, matches.size())))
                .currentPage(current).pageSize(pageSize).totalRecords(matches.size()).totalPages(totalPages)
                .hasNext(current < totalPages).hasPrevious(current > 1).build());
    }

    @Override
    public ApiResponse<ProductResponse> getProductById(Integer id) {
        log.debug("Fetching product details for ID={}", id);

        Optional<ProductResponse> productOpt = productSpRepository.callLayChiTietSanPham(id);
        return productOpt
                .filter(product -> !Boolean.TRUE.equals(product.getTrangThaiXoa()))
                .map(prod -> ApiResponse.ok("Lấy thông tin chi tiết sản phẩm thành công", prod))
                .orElseGet(() -> ApiResponse.error(-1, "Không tìm thấy sản phẩm có mã: " + id));
    }
}
