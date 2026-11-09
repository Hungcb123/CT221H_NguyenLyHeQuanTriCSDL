package com.ctu.freshgrocery.admin;

import com.ctu.freshgrocery.dto.*;
import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.model.*;
import com.ctu.freshgrocery.repository.*;
import com.ctu.freshgrocery.service.*;
import com.ctu.freshgrocery.service.impl.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final JdbcTemplate jdbc;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final PromotionRepository promotions;
    private final SemanticSearchService semantic;
    private final EmbeddingServiceImpl embedding;
    private final DemandForecastServiceImpl forecast;
    private final BenchmarkService benchmark;
    private final OrderService orders;
    private final AdminOrderService orderAdmin;

    @GetMapping("/overview")
    public ApiResponse<?> overview() {
        return ApiResponse.ok("Tổng quan quản trị", Map.of(
            "products", jdbc.queryForObject("SELECT COUNT(*) FROM SanPham WHERE TrangThaiXoa = 0", Long.class),
            "lowStock", jdbc.queryForObject("SELECT COUNT(*) FROM SanPham WHERE TrangThaiXoa = 0 AND SoLuongTon <= 5", Long.class),
            "expiring", jdbc.queryForObject("SELECT COUNT(*) FROM SanPham WHERE TrangThaiXoa = 0 AND HanSuDung <= DATEADD(DAY, 2, GETDATE())", Long.class),
            "orders", jdbc.queryForObject("SELECT COUNT_BIG(*) FROM DonHang d WHERE EXISTS (SELECT 1 FROM ChiTietDonHang c WHERE c.MaDH = d.MaDH)", Long.class),
            "revenue", jdbc.queryForObject("SELECT ISNULL(SUM(TongTien),0) FROM DonHang d WHERE TrangThai IN ('PAID','DELIVERED') AND EXISTS (SELECT 1 FROM ChiTietDonHang c WHERE c.MaDH = d.MaDH)", java.math.BigDecimal.class),
            "benchmarkOrders", jdbc.queryForObject("SELECT COUNT_BIG(*) FROM DonHang d WHERE NOT EXISTS (SELECT 1 FROM ChiTietDonHang c WHERE c.MaDH = d.MaDH)", Long.class),
            "recentAlerts", jdbc.queryForList("SELECT TOP 8 MaLog, LoaiSuKien, NoiDung, MaSP, NguoiThucHien, NgayGhi FROM NhatKyCanhBao ORDER BY MaLog DESC")));
    }

    @GetMapping("/products")
    public ApiResponse<?> products(@RequestParam(defaultValue="") String query, @RequestParam(defaultValue="false") boolean deleted,
                                  @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size) {
        var catalog = products.findAll().stream().filter(p -> Boolean.TRUE.equals(p.getTrangThaiXoa()) == deleted).toList();
        var scores = ProductBm25.score(catalog, query);
        var list = catalog.stream().filter(p -> query.isBlank() || scores.containsKey(p.getMaSP()) || query.equals(p.getMaSP().toString()))
                .sorted(Comparator.comparing(Product::getMaSP).reversed()).map(p -> {
                    Map<String,Object> row = new LinkedHashMap<>();
                    row.put("maSP",p.getMaSP()); row.put("tenSP",p.getTenSP()); row.put("moTa",p.getMoTa()); row.put("gia",p.getGia());
                    row.put("donViTinh",p.getDonViTinh()); row.put("soLuongTon",p.getSoLuongTon()); row.put("hanSuDung",p.getHanSuDung());
                    row.put("nhietDoBaoQuan",p.getNhietDoBaoQuan()); row.put("maDanhMuc",p.getMaDanhMuc()); row.put("trangThaiXoa",p.getTrangThaiXoa());
                    return row;
                }).toList();
        int current=Math.max(1,page), limit=Math.clamp(size,1,100), start=(int)Math.min((long)(current-1)*limit,list.size());
        return ApiResponse.ok("Danh sách quản trị", Map.of("items",list.subList(start,Math.min(start+limit,list.size())),"page",current,"size",limit,
                "totalElements",list.size(),"totalPages",(list.size()+limit-1)/limit));
    }

    @PostMapping("/products/{id}/restore")
    @Transactional
    public ApiResponse<?> restore(@PathVariable int id) {
        var product=products.findById(id).orElseThrow(()->new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy sản phẩm."));
        product.setTrangThaiXoa(false); product.setNgayCapNhat(LocalDateTime.now()); products.saveAndFlush(product); semantic.syncProduct(product);
        return ApiResponse.ok("Đã khôi phục sản phẩm", true);
    }

    public record CategoryInput(@NotBlank @Size(max=100) String tenDanhMuc, @Size(max=255) String moTa) {}
    @PostMapping("/categories")
    public ApiResponse<?> createCategory(@Valid @RequestBody CategoryInput body) {
        return ApiResponse.ok("Đã tạo danh mục", categories.save(Category.builder().tenDanhMuc(body.tenDanhMuc().trim()).moTa(body.moTa()).build()));
    }
    @PutMapping("/categories/{id}")
    public ApiResponse<?> updateCategory(@PathVariable int id,@Valid @RequestBody CategoryInput body) {
        var category=categories.findById(id).orElseThrow(()->new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy danh mục."));
        category.setTenDanhMuc(body.tenDanhMuc().trim());category.setMoTa(body.moTa());
        return ApiResponse.ok("Đã cập nhật danh mục",categories.save(category));
    }
    @DeleteMapping("/categories/{id}")
    @Transactional
    public ApiResponse<?> deleteCategory(@PathVariable int id) {
        if(!categories.existsById(id))throw new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy danh mục.");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM SanPham WHERE MaDanhMuc = ?",Integer.class,id)>0)
            throw new AppException(HttpStatus.CONFLICT,"Danh mục còn sản phẩm, kể cả sản phẩm đã ẩn; hãy chuyển sản phẩm trước.");
        categories.deleteById(id);return ApiResponse.ok("Đã xóa danh mục trống",true);
    }

    @GetMapping("/orders")
    public ApiResponse<?> orders(@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="false") boolean includeBenchmark,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        if(!status.isBlank()&&!Set.of("PENDING","PAID","DELIVERED","CANCELLED").contains(status))throw new AppException(HttpStatus.BAD_REQUEST,"Trạng thái không hợp lệ.");
        int limit=Math.clamp(size,1,100);long start=(long)(Math.max(1,page)-1)*limit;
        String where=" WHERE (? = '' OR d.TrangThai = ?) AND (? = 1 OR EXISTS (SELECT 1 FROM ChiTietDonHang c WHERE c.MaDH=d.MaDH))";
        Long total=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM DonHang d"+where,Long.class,status,status,includeBenchmark?1:0);
        var items=jdbc.queryForList("SELECT d.MaDH, d.MaKH, k.HoTen, d.NgayTao, d.TongTien, d.TrangThai, d.DiaChiGiaoHang FROM DonHang d JOIN KhachHang k ON k.MaKH=d.MaKH"+where+" ORDER BY d.MaDH DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",status,status,includeBenchmark?1:0,start,limit);
        return ApiResponse.ok("Danh sách đơn hàng",Map.of("items",items,"totalElements",total,"page",Math.max(1,page),"size",limit,"totalPages",(total+limit-1)/limit));
    }
    @GetMapping("/orders/{id}")
    public ApiResponse<?> order(@PathVariable int id){return ApiResponse.ok("Chi tiết đơn",orders.getOrderById(id));}
    public record StatusInput(@NotBlank String status) {}
    @PatchMapping("/orders/{id}/status")
    public ApiResponse<?> status(@PathVariable int id,@Valid @RequestBody StatusInput body,Authentication auth){
        return ApiResponse.ok("Đã cập nhật trạng thái đơn",orderAdmin.changeStatus(id,body.status(),auth.getName()));
    }

    @PutMapping("/promotions/{id}")
    public ApiResponse<?> updatePromotion(@PathVariable int id,@Valid @RequestBody PromotionCreateRequest body) {
        if(!body.getNgayKetThuc().isAfter(body.getNgayBatDau()))throw new AppException(HttpStatus.BAD_REQUEST,"Ngày kết thúc phải sau ngày bắt đầu.");
        var promotion=promotions.findById(id).orElseThrow(()->new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy khuyến mãi."));
        promotion.setTenKM(body.getTenKM());promotion.setMoTa(body.getMoTa());promotion.setLoaiKM(body.getLoaiKM());
        promotion.setNgayBatDau(body.getNgayBatDau());promotion.setNgayKetThuc(body.getNgayKetThuc());promotion.setNgayCapNhat(LocalDateTime.now());
        promotions.save(promotion);return ApiResponse.ok("Đã cập nhật khuyến mãi",true);
    }
    public record ActiveInput(boolean active) {}
    @PatchMapping("/promotions/{id}/active")
    public ApiResponse<?> promotionActive(@PathVariable int id,@RequestBody ActiveInput body) {
        var promotion=promotions.findById(id).orElseThrow(()->new AppException(HttpStatus.NOT_FOUND,"Không tìm thấy khuyến mãi."));
        promotion.setTrangThai(body.active());promotion.setNgayCapNhat(LocalDateTime.now());promotions.save(promotion);
        return ApiResponse.ok("Đã cập nhật trạng thái khuyến mãi",true);
    }
    @DeleteMapping("/promotions/{id}/products/{productId}")
    public ApiResponse<?> removePromotionProduct(@PathVariable int id,@PathVariable int productId) {
        jdbc.update("DELETE FROM ChiTietKhuyenMai WHERE MaKM = ? AND MaSP = ?",id,productId);
        return ApiResponse.ok("Đã gỡ sản phẩm khỏi khuyến mãi",true);
    }

    @GetMapping("/customers")
    public ApiResponse<?> customers(){return ApiResponse.ok("Khách hàng",jdbc.queryForList("SELECT MaKH, HoTen, Email, SoDienThoai, DiaChi, SoDuVi FROM KhachHang ORDER BY MaKH"));}
    @GetMapping("/customers/{id}/wallet")
    public ApiResponse<?> wallet(@PathVariable int id){return ApiResponse.ok("Lịch sử ví",jdbc.queryForList("SELECT TOP 100 * FROM LichSuGiaoDichVi WHERE MaKH=? ORDER BY MaGiaoDich DESC",id));}

    @GetMapping("/ai/status")
    public ApiResponse<?> aiStatus(){return ApiResponse.ok("Trạng thái AI",Map.of("embedding",Map.of("ready",embedding.isReady(),"name","all-MiniLM-L6-v2","dimension",embedding.getDimension()),
            "qdrant",semantic.getStatus(),"forecast",forecast.getModelStatus()));}
    @PostMapping("/ai/forecast/{id}")
    public ApiResponse<?> predict(@PathVariable int id){return ApiResponse.ok("Đã suy luận và lưu dự báo",forecast.predictForProduct(id));}
    @PostMapping("/benchmark/compare")
    public ApiResponse<?> compare(){return ApiResponse.ok("Đã đo hiệu năng thực tế",benchmark.compareBenchmark());}
}
