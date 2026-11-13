package com.ctu.freshgrocery.service;
import com.ctu.freshgrocery.dto.*;
import com.ctu.freshgrocery.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

/** Each product owns its types. Child IDs are the sellable inventory/cart IDs. */
@Service @RequiredArgsConstructor
public class ProductTypeService {
    private final JdbcTemplate jdbc;

    public List<Integer> childIds(int parent, boolean includeHidden) {
        return jdbc.queryForList("SELECT MaSP FROM SanPham WHERE MaSPCha = ?" +
                (includeHidden ? "" : " AND TrangThaiXoa = 0") + " ORDER BY MaSP", Integer.class, parent);
    }

    public Map<String, Object> metadata(int id) {
        var rows = jdbc.queryForList("SELECT s.MaSPCha AS maSPCha, s.TenLoai AS tenLoai, p.TenSP AS tenSanPhamCha FROM SanPham s LEFT JOIN SanPham p ON p.MaSP = s.MaSPCha WHERE s.MaSP = ?", id);
        return rows.isEmpty() ? Collections.emptyMap() : rows.getFirst();
    }

    public void enrich(List<ProductResponse> products) {
        if (products.isEmpty()) return;
        var placeholders = String.join(",", Collections.nCopies(products.size(), "?"));
        var rows = jdbc.queryForList("SELECT s.MaSP, s.MaSPCha, s.TenLoai, p.TenSP AS TenSanPhamCha FROM SanPham s LEFT JOIN SanPham p ON p.MaSP = s.MaSPCha WHERE s.MaSP IN (" + placeholders + ")", products.stream().map(ProductResponse::getMaSP).toArray());
        var byId = new HashMap<Integer, Map<String, Object>>();
        rows.forEach(row -> byId.put((Integer) row.get("MaSP"), row));
        var statuses = saleStatuses(products.stream().map(ProductResponse::getMaSP).toList());
        products.forEach(p -> {
            p.setTrangThaiBan(statuses.get(p.getMaSP()));
            var row = byId.get(p.getMaSP());
            if (row != null) {
                p.setMaSPCha((Integer) row.get("MaSPCha"));
                p.setTenLoai((String) row.get("TenLoai"));
                p.setTenSanPhamCha((String) row.get("TenSanPhamCha"));
            }
        });
    }

    public Map<Integer, String> saleStatuses(Collection<Integer> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        var placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        var rows = jdbc.queryForList("""
                SELECT s.MaSP, CASE
                  WHEN s.TrangThaiXoa = 1 OR p.TrangThaiXoa = 1 THEN 'STOPPED'
                  WHEN EXISTS (SELECT 1 FROM SanPham c WHERE c.MaSPCha = s.MaSP AND c.TrangThaiXoa = 0)
                    THEN CASE WHEN EXISTS (SELECT 1 FROM SanPham c WHERE c.MaSPCha = s.MaSP
                           AND c.TrangThaiXoa = 0 AND c.HanSuDung > GETDATE() AND c.SoLuongTon > 0)
                         THEN 'AVAILABLE' ELSE 'NO_AVAILABLE_TYPES' END
                  WHEN s.HanSuDung IS NULL THEN 'UNKNOWN_EXPIRY'
                  WHEN s.HanSuDung <= GETDATE() THEN 'EXPIRED'
                  WHEN s.SoLuongTon <= 0 THEN 'OUT_OF_STOCK'
                  ELSE 'AVAILABLE' END AS TrangThaiBan
                FROM SanPham s LEFT JOIN SanPham p ON p.MaSP = s.MaSPCha
                WHERE s.MaSP IN (
                """ + placeholders + ")", ids.toArray());
        var result = new HashMap<Integer, String>();
        rows.forEach(row -> result.put((Integer) row.get("MaSP"), (String) row.get("TrangThaiBan")));
        return result;
    }

    // Runs in the same transaction as creating/updating the owning product.
    public List<Integer> save(int parentId, List<ProductTypeRequest> requests) {
        if (requests == null) {
            jdbc.update("UPDATE c SET c.TenSP = p.TenSP + N' - ' + c.TenLoai, c.MaDanhMuc = p.MaDanhMuc FROM SanPham c JOIN SanPham p ON p.MaSP = c.MaSPCha WHERE p.MaSP = ?", parentId);
            return childIds(parentId, true);
        }
        var parent = jdbc.queryForMap("SELECT * FROM SanPham WITH (UPDLOCK, HOLDLOCK) WHERE MaSP = ? AND TrangThaiXoa = 0", parentId);
        if (parent.get("MaSPCha") != null) bad("Không thêm loại bên trong một loại khác.");
        var existing = new HashSet<>(childIds(parentId, true));
        var names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        var ids = new HashSet<Integer>();
        for (var req : requests) {
            if (req.getMaSP() != null && (!existing.contains(req.getMaSP()) || !ids.add(req.getMaSP())))
                bad("Loại không thuộc sản phẩm đang sửa hoặc bị lặp mã.");
            if (!req.isTrangThaiXoa() && !names.add(req.getTenLoai().trim())) bad("Tên các loại trong một sản phẩm không được trùng nhau.");
            if (req.getMaSP() == null && !req.getHanSuDung().isAfter(LocalDateTime.now())) bad("Hạn dùng của loại mới phải ở tương lai.");
        }
        // Hide first so existing type names can be swapped under the unique index.
        jdbc.update("UPDATE SanPham SET TrangThaiXoa = 1 WHERE MaSPCha = ?", parentId);
        for (var req : requests) {
            var name = parent.get("TenSP") + " - " + req.getTenLoai().trim();
            if (name.length() > 200) bad("Tên sản phẩm và tên loại cộng lại không được quá 200 ký tự.");
            if (req.getMaSP() == null) {
                jdbc.update("INSERT INTO SanPham (MaSPCha, TenLoai, TenSP, MoTa, HinhAnh, Gia, DonViTinh, SoLuongTon, HanSuDung, NhietDoBaoQuan, MaDanhMuc, TrangThaiXoa, NgayTao, NgayCapNhat) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())",
                        parentId, req.getTenLoai().trim(), name, req.getMoTa(), req.getHinhAnh(), req.getGia(), req.getDonViTinh().trim(), req.getSoLuongTon(), req.getHanSuDung(), req.getNhietDoBaoQuan(), parent.get("MaDanhMuc"), req.isTrangThaiXoa());
            } else {
                jdbc.update("UPDATE SanPham SET TenLoai = ?, TenSP = ?, MoTa = ?, HinhAnh = ?, Gia = ?, DonViTinh = ?, SoLuongTon = ?, HanSuDung = ?, NhietDoBaoQuan = ?, MaDanhMuc = ?, TrangThaiXoa = ?, NgayCapNhat = GETDATE() WHERE MaSP = ? AND MaSPCha = ?",
                        req.getTenLoai().trim(), name, req.getMoTa(), req.getHinhAnh(), req.getGia(), req.getDonViTinh().trim(), req.getSoLuongTon(), req.getHanSuDung(), req.getNhietDoBaoQuan(), parent.get("MaDanhMuc"), req.isTrangThaiXoa(), req.getMaSP(), parentId);
            }
        }
        return childIds(parentId, true);
    }
    private static void bad(String message) { throw new AppException(HttpStatus.BAD_REQUEST, message); }
}
