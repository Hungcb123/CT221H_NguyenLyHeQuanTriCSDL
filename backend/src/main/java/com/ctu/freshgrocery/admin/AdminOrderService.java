package com.ctu.freshgrocery.admin;

import com.ctu.freshgrocery.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminOrderService {
    private final JdbcTemplate jdbc;

    @Transactional
    public Map<String, Object> changeStatus(int id, String target, String actor) {
        if (!Set.of("DELIVERED", "CANCELLED").contains(target))
            throw new AppException(HttpStatus.BAD_REQUEST, "Chỉ được giao hàng hoặc hủy đơn; thanh toán phải qua giao dịch checkout.");
        var owners = jdbc.queryForList("SELECT MaKH FROM DonHang WHERE MaDH = ? AND NguonDon = 'SALE'", id);
        if (owners.isEmpty()) throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng.");
        int customer = ((Number) owners.getFirst().get("MaKH")).intValue();
        // Same customer -> inventory lock order as checkout; all refund effects share one transaction.
        BigDecimal balance = jdbc.queryForObject("SELECT SoDuVi FROM KhachHang WITH (UPDLOCK, ROWLOCK) WHERE MaKH = ?", BigDecimal.class, customer);
        var order = jdbc.queryForMap("SELECT TrangThai, TongTien, PhuongThucThanhToan FROM DonHang WITH (UPDLOCK, ROWLOCK) WHERE MaDH = ?", id);
        String current = (String) order.get("TrangThai");
        if (target.equals(current)) return Map.of("maDH", id, "trangThai", current, "refunded", false);
        if("CANCELLED".equals(target) && "MOMO_SANDBOX".equals(order.get("PhuongThucThanhToan")))
            throw new AppException(HttpStatus.CONFLICT,"Đơn MoMo phải được hủy/hoàn qua cổng MoMo và đối soát. Không hoàn vào ví DailyMart.");
        boolean valid = ("PAID".equals(current) && Set.of("DELIVERED", "CANCELLED").contains(target))
                || ("PENDING".equals(current) && "CANCELLED".equals(target));
        if (!valid) throw new AppException(HttpStatus.CONFLICT, "Không thể chuyển trạng thái đơn từ " + current + " sang " + target);
        boolean refunded = "PAID".equals(current) && "CANCELLED".equals(target);
        if (refunded) {
            var items = jdbc.queryForList("SELECT MaSP, SoLuong, MaKM FROM ChiTietDonHang WHERE MaDH = ? ORDER BY MaSP", id);
            if (items.isEmpty()) throw new AppException(HttpStatus.CONFLICT, "Đơn không có chi tiết; không được hoàn ví cho dữ liệu benchmark.");
            BigDecimal amount = (BigDecimal) order.get("TongTien");
            jdbc.update("UPDATE KhachHang SET SoDuVi = SoDuVi + ? WHERE MaKH = ?", amount, customer);
            for (var item : items) {
                jdbc.update("UPDATE SanPham SET SoLuongTon = SoLuongTon + ?, NgayCapNhat = GETDATE() WHERE MaSP = ?", item.get("SoLuong"), item.get("MaSP"));
                if (item.get("MaKM") != null) jdbc.update("UPDATE ChiTietKhuyenMai SET SoLuongDaBan = CASE WHEN SoLuongDaBan >= ? THEN SoLuongDaBan - ? ELSE 0 END WHERE MaKM = ? AND MaSP = ?",
                        item.get("SoLuong"), item.get("SoLuong"), item.get("MaKM"), item.get("MaSP"));
            }
            jdbc.update("INSERT INTO LichSuGiaoDichVi (MaKH, MaDH, LoaiGiaoDich, SoDuTruoc, SoTienThayDoi, SoDuSau, NoiDung) VALUES (?, ?, 'HOAN_TIEN', ?, ?, ?, ?)",
                    customer, id, balance, amount, balance.add(amount), "Hoàn ví do quản trị hủy đơn #" + id);
        }
        jdbc.update("UPDATE DonHang SET TrangThai = ? WHERE MaDH = ?", target, id);
        jdbc.update("INSERT INTO NhatKyCanhBao (LoaiSuKien, NoiDung, NguoiThucHien) VALUES ('ADMIN_ORDER_STATUS', ?, ?)",
                "Đơn #" + id + ": " + current + " -> " + target + (refunded ? "; đã hoàn ví và kho" : ""), actor);
        return Map.of("maDH", id, "trangThai", target, "refunded", refunded);
    }
}
