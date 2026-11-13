package com.ctu.freshgrocery.admin;

import com.ctu.freshgrocery.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesForecastService {
    private final JdbcTemplate jdbc;

    public record DayEstimate(LocalDate date, BigDecimal revenue) {}
    public record Estimate(String source, int historyDays, int sellingDays, BigDecimal total, List<DayEstimate> days) {}

    private record Sale(int productId, LocalDate date, long quantity) {}
    private record Item(int id, BigDecimal price, BigDecimal stock, LocalDate expires) {}
    private record Offer(int id, int productId, BigDecimal price, BigDecimal quota,
                         LocalDateTime starts, LocalDateTime ends) {}

    public Estimate estimate(int days, String source) {
        if (!Set.of(1, 7, 30).contains(days) || !Set.of("SALE", "DEMO_FORECAST").contains(source)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chọn kỳ 1, 7 hoặc 30 ngày và nguồn dữ liệu hợp lệ.");
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate tomorrow = today.plusDays(1);
        var sales = jdbc.query("""
                SELECT c.MaSP, CAST(d.NgayTao AS DATE) AS NgayBan, SUM(CONVERT(BIGINT, c.SoLuong)) AS SoLuong
                FROM DonHang d JOIN ChiTietDonHang c ON c.MaDH = d.MaDH
                WHERE d.NguonDon = ? AND d.TrangThai IN ('PAID','DELIVERED')
                  AND d.NgayTao >= ? AND d.NgayTao < ?
                GROUP BY c.MaSP, CAST(d.NgayTao AS DATE)
                """, (rs, row) -> new Sale(rs.getInt("MaSP"), rs.getDate("NgayBan").toLocalDate(), rs.getLong("SoLuong")),
                source, Date.valueOf(today.minusDays(55)), Date.valueOf(tomorrow));

        if (sales.isEmpty()) return new Estimate(source, 0, 0, BigDecimal.ZERO, List.of());

        LocalDate firstDay = sales.stream().map(Sale::date).min(LocalDate::compareTo).orElseThrow();
        Map<DayOfWeek, Long> weekdayCounts = firstDay.datesUntil(tomorrow)
                .collect(Collectors.groupingBy(LocalDate::getDayOfWeek, Collectors.counting()));
        int historyDays = weekdayCounts.values().stream().mapToInt(Long::intValue).sum();
        int sellingDays = (int) sales.stream().map(Sale::date).distinct().count();
        var salesByProduct = sales.stream().collect(Collectors.groupingBy(Sale::productId));

        var items = jdbc.query("""
                SELECT MaSP, Gia, SoLuongTon, CAST(HanSuDung AS DATE) AS Han
                FROM SanPham WHERE TrangThaiXoa = 0 AND SoLuongTon > 0 AND HanSuDung >= ?
                """, (rs, row) -> new Item(rs.getInt("MaSP"), rs.getBigDecimal("Gia"),
                rs.getBigDecimal("SoLuongTon"), rs.getDate("Han").toLocalDate()), Date.valueOf(tomorrow));
        var offers = jdbc.query("""
                SELECT c.MaKM, c.MaSP, c.GiaKhuyenMai, c.SoLuongKhuyenMai - c.SoLuongDaBan AS ConLai,
                       p.NgayBatDau, p.NgayKetThuc
                FROM ChiTietKhuyenMai c JOIN ChuongTrinhKhuyenMai p ON p.MaKM = c.MaKM
                WHERE p.TrangThai = 1 AND p.NgayKetThuc >= ? AND p.NgayBatDau < ?
                  AND (c.SoLuongKhuyenMai IS NULL OR c.SoLuongKhuyenMai > c.SoLuongDaBan)
                ORDER BY c.GiaKhuyenMai, c.MaKM
                """, (rs, row) -> new Offer(rs.getInt("MaKM"), rs.getInt("MaSP"), rs.getBigDecimal("GiaKhuyenMai"),
                rs.getBigDecimal("ConLai"), rs.getTimestamp("NgayBatDau").toLocalDateTime(),
                rs.getTimestamp("NgayKetThuc").toLocalDateTime()), Date.valueOf(tomorrow), Date.valueOf(tomorrow.plusDays(days)));
        var offersByProduct = offers.stream().collect(Collectors.groupingBy(Offer::productId));

        Map<LocalDate, BigDecimal> daily = new HashMap<>();
        for (Item item : items) {
            var productSales = salesByProduct.getOrDefault(item.id(), List.of());
            if (productSales.isEmpty()) continue;
            var salesByWeekday = productSales.stream().collect(Collectors.groupingBy(
                    sale -> sale.date().getDayOfWeek(), Collectors.summingLong(Sale::quantity)));
            long allSales = productSales.stream().mapToLong(Sale::quantity).sum();
            Map<Integer, BigDecimal> quotas = new HashMap<>();
            var productOffers = offersByProduct.getOrDefault(item.id(), List.of());
            productOffers.forEach(offer -> quotas.put(offer.id(), offer.quota()));
            BigDecimal stock = item.stock();
            for (int offset = 0; offset < days && stock.signum() > 0; offset++) {
                LocalDate day = tomorrow.plusDays(offset);
                if (!day.isBefore(item.expires())) break;
                long weekdayDays = weekdayCounts.getOrDefault(day.getDayOfWeek(), 0L);
                long units = weekdayDays == 0 ? allSales : salesByWeekday.getOrDefault(day.getDayOfWeek(), 0L);
                BigDecimal demand = BigDecimal.valueOf(units)
                        .divide(BigDecimal.valueOf(weekdayDays == 0 ? historyDays : weekdayDays), 4, RoundingMode.HALF_UP)
                        .min(stock);
                stock = stock.subtract(demand);
                BigDecimal value = valueAtAvailablePrices(item.price(), demand, day, productOffers, quotas);
                daily.merge(day, value, BigDecimal::add);
            }
        }

        List<DayEstimate> estimates = new ArrayList<>();
        for (int offset = 0; offset < days; offset++) {
            LocalDate date = tomorrow.plusDays(offset);
            estimates.add(new DayEstimate(date, daily.getOrDefault(date, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)));
        }
        BigDecimal total = estimates.stream().map(DayEstimate::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Estimate(source, historyDays, sellingDays, total, estimates);
    }

    private BigDecimal valueAtAvailablePrices(BigDecimal price, BigDecimal demand, LocalDate day,
                                              List<Offer> offers, Map<Integer, BigDecimal> quotas) {
        BigDecimal revenue = BigDecimal.ZERO;
        for (Offer offer : offers) {
            if (demand.signum() == 0 || offer.price().compareTo(price) >= 0) break;
            if (offer.starts().isAfter(day.atTime(12, 0)) || offer.ends().isBefore(day.atTime(12, 0))) continue;
            BigDecimal quota = quotas.get(offer.id());
            BigDecimal discounted = quota == null ? demand : demand.min(quota);
            demand = demand.subtract(discounted);
            revenue = revenue.add(discounted.multiply(offer.price()));
            if (quota != null) quotas.put(offer.id(), quota.subtract(discounted));
        }
        return revenue.add(demand.multiply(price));
    }
}
