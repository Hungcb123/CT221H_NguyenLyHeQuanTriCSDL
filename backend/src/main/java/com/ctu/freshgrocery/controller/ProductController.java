package com.ctu.freshgrocery.controller;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.dto.ProductCreateRequest;
import com.ctu.freshgrocery.dto.ProductPageResponse;
import com.ctu.freshgrocery.dto.ProductResponse;
import com.ctu.freshgrocery.dto.ProductUpdateRequest;
import com.ctu.freshgrocery.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "1. Quản lý Thực phẩm tươi sống (Product Module)", description = "CRUD Thực phẩm tươi sống qua Stored Procedures & Triggers kiểm soát")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(
            summary = "Thêm mới thực phẩm tươi sống (Gọi SP: sp_ThemSanPhamMoi)",
            description = """
                    Gọi Stored Procedure **sp_ThemSanPhamMoi** với tham số kiểm soát `IN` và `OUTPUT`.
                    Kiểm tra các ràng buộc nghiệp vụ:
                    - Giá > 0 VND (nếu <= 0, SP trả về StatusCode = -1)
                    - Số lượng tồn >= 0 (nếu < 0, SP trả về StatusCode = -2)
                    - Hạn sử dụng phải ở tương lai (nếu <= hiện tại, SP trả về StatusCode = -3)
                    - Mã danh mục phải tồn tại (nếu sai, SP trả về StatusCode = -4)
                    - Bắt lỗi runtime qua khối `BEGIN TRY ... BEGIN CATCH`.
                    """
    )
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        ApiResponse<ProductResponse> response = productService.createProduct(request);
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Cập nhật sản phẩm & Test Trigger bẫy giá / hết hàng (Gọi SP: sp_CapNhatSanPham)",
            description = """
                    Gọi Stored Procedure **sp_CapNhatSanPham**.
                    
                    💡 **Kịch bản kiểm thử Trigger tự động (trg_SanPham_CanhBaoBienDong):**
                    - **Test bẫy biến động giá bất thường:** Thử giảm giá xuống dưới 50% hoặc tăng vọt trên 200% so với giá cũ -> Trigger tự động sinh bản ghi `CANH_BAO_GIA_BAT_THUONG` trong bảng `NhatKyCanhBao`.
                    - **Test bẫy cháy hàng:** Thử cập nhật `soLuongTon = 0` -> Trigger tự động sinh bản ghi `CANH_BAO_HET_HANG` vào `NhatKyCanhBao`.
                    - **Test bẫy cận date:** Thử chỉnh `hanSuDung` còn <= 1 ngày -> Trigger tự động sinh bản ghi `CANH_BAO_CAN_DATE`.
                    """
    )
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "Mã sản phẩm cần cập nhật", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody ProductUpdateRequest request) {
        ApiResponse<ProductResponse> response = productService.updateProduct(id, request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Xóa sản phẩm - Test Trigger chặn xóa vật lý Soft Delete (Trigger: trg_SanPham_NganChanXoaVatLy)",
            description = """
                    Thực thi câu lệnh `DELETE FROM SanPham WHERE MaSP = {id}`.
                    
                    💡 **Kịch bản kiểm thử Trigger INSTEAD OF DELETE:**
                    - Lệnh xóa vật lý bị chặn hoàn toàn!
                    - Trigger tự động chuyển đổi thành cập nhật cờ `TrangThaiXoa = 1`.
                    - Bảng `NhatKyCanhBao` tự động sinh bản ghi sự kiện `XOA_MEM_SAN_PHAM`.
                    """
    )
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @Parameter(description = "Mã sản phẩm muốn xóa", example = "1")
            @PathVariable Integer id) {
        ApiResponse<String> response = productService.deleteProduct(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Truy vấn danh sách sản phẩm phân trang & lọc (Gọi SP: sp_LayDanhSachSanPhamPhanTrang)",
            description = """
                    Gọi Stored Procedure **sp_LayDanhSachSanPhamPhanTrang** hỗ trợ:
                    - Phân trang bằng `OFFSET ... FETCH NEXT`
                    - Lọc theo danh mục (`categoryId`)
                    - Tìm kiếm theo từ khóa tên hoặc mô tả (`keyword`)
                    - Lọc theo khoảng giá (`minPrice` - `maxPrice`)
                    - Sắp xếp: `MOI_NHAT`, `GIA_ASC`, `GIA_DESC`, `HSD_ASC`, `TON_DESC`.
                    - Trả về tổng số bản ghi và tổng số trang qua các tham số `OUTPUT`.
                    """
    )
    public ResponseEntity<ApiResponse<ProductPageResponse>> getProducts(
            @Parameter(description = "Mã danh mục lọc (để trống nếu lấy tất cả)")
            @RequestParam(required = false) Integer categoryId,
            @Parameter(description = "Từ khóa tìm kiếm gần đúng")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Giá tối thiểu (VND)")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Giá tối đa (VND)")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Tiêu chí sắp xếp (MOI_NHAT, GIA_ASC, GIA_DESC, HSD_ASC, TON_DESC)")
            @RequestParam(required = false, defaultValue = "MOI_NHAT") String sortBy,
            @Parameter(description = "Số trang (bắt đầu từ 1)")
            @RequestParam(required = false, defaultValue = "1") int page,
            @Parameter(description = "Số dòng mỗi trang")
            @RequestParam(required = false, defaultValue = "10") int size) {
        ApiResponse<ProductPageResponse> response = productService.getProducts(
                categoryId, keyword, minPrice, maxPrice, sortBy, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy thông tin chi tiết một sản phẩm",
            description = "Gọi Stored Procedure **sp_LayChiTietSanPham** theo mã ID."
    )
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "Mã sản phẩm", example = "1")
            @PathVariable Integer id) {
        ApiResponse<ProductResponse> response = productService.getProductById(id);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
