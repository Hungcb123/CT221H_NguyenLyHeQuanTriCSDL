package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.ProductResponse;
import com.ctu.freshgrocery.repository.ProductSpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** Publish only real, purchasable SKU offers; never inherit a parent's discount. */
@Service
@RequiredArgsConstructor
public class PromotionStorefrontService {
    private final JdbcTemplate jdbc;
    private final ProductSpRepository products;

    public record Campaign(Integer maKM, String tenKM, String moTa, String loaiKM,
                           LocalDateTime ngayKetThuc, List<ProductResponse> products) {}

    @Transactional(readOnly = true)
    public List<Campaign> getOffers() {
        var rows = jdbc.queryForList("""
            SELECT c.MaKM, c.TenKM, c.MoTa, c.LoaiKM, c.NgayKetThuc, s.MaSP
            FROM ChuongTrinhKhuyenMai c
            JOIN ChiTietKhuyenMai d ON d.MaKM = c.MaKM
            JOIN SanPham s ON s.MaSP = d.MaSP
            LEFT JOIN SanPham parent ON parent.MaSP = s.MaSPCha
            WHERE c.TrangThai = 1 AND GETDATE() BETWEEN c.NgayBatDau AND c.NgayKetThuc
              AND s.TrangThaiXoa = 0 AND ISNULL(parent.TrangThaiXoa, 0) = 0
              AND s.HanSuDung > GETDATE() AND s.SoLuongTon > 0
              AND d.GiaKhuyenMai > 0 AND d.GiaKhuyenMai < s.Gia
              AND (d.SoLuongKhuyenMai IS NULL OR d.SoLuongDaBan < d.SoLuongKhuyenMai)
              AND NOT EXISTS (SELECT 1 FROM SanPham child WHERE child.MaSPCha = s.MaSP)
            ORDER BY CASE WHEN c.LoaiKM = 'FLASH_SALE' THEN 0 ELSE 1 END, c.NgayKetThuc, c.MaKM, s.MaSP
            """);
        var campaigns = new LinkedHashMap<Integer, Campaign>();
        var loaded = new HashMap<Integer, Optional<ProductResponse>>();
        for (var row : rows) {
            int id = (Integer) row.get("MaSP");
            var product = loaded.computeIfAbsent(id, products::callLayChiTietSanPham).orElse(null);
            int campaignId = (Integer) row.get("MaKM");
            // Reuse checkout's best-price procedure and check again after the initial query.
            if (product == null || !Boolean.TRUE.equals(product.getDangKhuyenMai())
                    || !Objects.equals(product.getMaKM(), campaignId)
                    || !"AVAILABLE".equals(product.getTrangThaiBan())) continue;
            var campaign = campaigns.computeIfAbsent(campaignId, key -> new Campaign(key,
                    (String) row.get("TenKM"), (String) row.get("MoTa"), (String) row.get("LoaiKM"),
                    ((java.sql.Timestamp) row.get("NgayKetThuc")).toLocalDateTime(), new ArrayList<>()));
            campaign.products().add(product);
        }
        return List.copyOf(campaigns.values());
    }
}
