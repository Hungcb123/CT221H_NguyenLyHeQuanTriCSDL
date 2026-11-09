package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.BenchmarkComparisonResponse;
import com.ctu.freshgrocery.service.BenchmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkServiceImpl implements BenchmarkService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public BenchmarkComparisonResponse compareBenchmark() {
        log.info("Bắt đầu thực thi đo kiểm hiệu năng chỉ mục CSDL (Benchmark Comparison)...");

        // 1. Kiểm tra số lượng bản ghi hiện có
        Long totalCount = jdbcTemplate.queryForObject("SELECT COUNT_BIG(1) FROM DonHang", Long.class);
        if (totalCount == null) totalCount = 0L;
        Long benchmarkCount = jdbcTemplate.queryForObject("SELECT COUNT_BIG(*) FROM DonHang WHERE NguonDon = 'BENCHMARK'", Long.class);

        // Đảm bảo chỉ mục tồn tại trước khi đo
        ensureCoveringIndexExists();

        // 2. Kịch bản A: KHÔNG dùng Index (Ép quét bảng tuần tự Clustered Index Scan)
        String sqlNoIndex = """
                SELECT 
                    dh.MaKH,
                    COUNT(dh.MaDH) AS SoDonHang,
                    SUM(dh.TongTien) AS TongChiTieu
                FROM DonHang dh WITH (INDEX(0))
                WHERE dh.NgayTao >= DATEADD(MONTH, -6, GETDATE()) 
                  AND dh.TrangThai IN ('PAID', 'DELIVERED')
                  AND dh.NguonDon = 'BENCHMARK'
                GROUP BY dh.MaKH
                """;

        long startScan = System.currentTimeMillis();
        List<Map<String, Object>> resScan = jdbcTemplate.queryForList(sqlNoIndex);
        long elapsedScan = System.currentTimeMillis() - startScan;

        // 3. Kịch bản B: CÓ Covering Index (Ép tìm kiếm nhị phân B-Tree Seek)
        String sqlWithIndex = """
                SELECT 
                    dh.MaKH,
                    COUNT(dh.MaDH) AS SoDonHang,
                    SUM(dh.TongTien) AS TongChiTieu
                FROM DonHang dh WITH (INDEX(IX_DonHang_NgayTao_TrangThai_Covering))
                WHERE dh.NgayTao >= DATEADD(MONTH, -6, GETDATE()) 
                  AND dh.TrangThai IN ('PAID', 'DELIVERED')
                  AND dh.NguonDon = 'BENCHMARK'
                GROUP BY dh.MaKH
                """;

        long startSeek = System.currentTimeMillis();
        List<Map<String, Object>> resSeek = jdbcTemplate.queryForList(sqlWithIndex);
        long elapsedSeek = System.currentTimeMillis() - startSeek;

        // Đảm bảo tối thiểu 1ms để tính toán tỷ lệ
        long safeSeek = Math.max(1, elapsedSeek);
        long safeScan = Math.max(1, elapsedScan);

        double speedup = (double) safeScan / safeSeek;
        double reduction = ((double) (safeScan - safeSeek) / safeScan) * 100.0;

        BenchmarkComparisonResponse.ScenarioResult scanResult = BenchmarkComparisonResponse.ScenarioResult.builder()
                .scenarioName("1. KHÔNG SỬ DỤNG CHỈ MỤC (NO_INDEX)")
                .accessMethod("Clustered Index Scan / Table Scan (Duyệt tuần tự toàn bộ bảng)")
                .executionTimeMs(elapsedScan)
                .recordsReturned(resScan.size())
                .executionPlanNote("Bộ tối ưu hóa truy vấn buộc phải quét toàn bộ các trang dữ liệu của bảng DonHang, gây tắc nghẽn bộ nhớ đệm (Buffer Pool) và tốn CPU.")
                .build();

        BenchmarkComparisonResponse.ScenarioResult seekResult = BenchmarkComparisonResponse.ScenarioResult.builder()
                .scenarioName("2. TỐI ƯU CHỈ MỤC BAO PHỦ (COVERING_INDEX)")
                .accessMethod("Truy vấn ép dùng covering index; cần execution plan để xác nhận Seek/Scan")
                .executionTimeMs(elapsedSeek)
                .recordsReturned(resSeek.size())
                .executionPlanNote("Dùng index hint trên chỉ mục bao phủ. Thời gian là wall-clock, chưa đo STATISTICS IO hoặc execution plan.")
                .build();

        String eval = String.format(
                "Tỷ lệ thời gian đo được: %.2fx; thay đổi %.2f%%. Đây là một lượt đo, không phải kết luận về execution plan hay I/O.",
                speedup, reduction
        );

        return BenchmarkComparisonResponse.builder()
                .totalOrdersInDatabase(totalCount)
                .benchmarkOrdersInDatabase(benchmarkCount)
                .withoutIndex(scanResult)
                .withCoveringIndex(seekResult)
                .speedupFactor(String.format("%.1fx", speedup))
                .timeReductionPercentage(String.format("%.1f%%", reduction))
                .academicEvaluation(eval)
                .build();
    }

    @Override
    public Map<String, Object> generateData(int count) {
        int safeCount = Math.min(Math.max(1000, count), 1000000);
        log.info("Kích hoạt sinh dữ liệu lớn: {} đơn hàng...", safeCount);

        String sql = String.format("""
                ;WITH 
                  E1(N) AS (SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1),
                  E2(N) AS (SELECT 1 FROM E1 a CROSS JOIN E1 b),
                  E3(N) AS (SELECT 1 FROM E2 a CROSS JOIN E2 b),
                  E4(N) AS (SELECT 1 FROM E3 a CROSS JOIN E1 b),
                  E5(N) AS (SELECT 1 FROM E4 a CROSS JOIN E1 b),
                  Numbers(N) AS (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) FROM E5)
                INSERT INTO DonHang (MaKH, NgayTao, TongTien, DiaChiGiaoHang, GhiChu, TrangThai, NguonDon)
                SELECT 
                    (ABS(CHECKSUM(NEWID())) %% 3) + 1 AS MaKH,
                    DATEADD(MINUTE, - (N %% 525600), GETDATE()) AS NgayTao,
                    ROUND(RAND(CHECKSUM(NEWID())) * (2000000 - 50000) + 50000, -3) AS TongTien,
                    N'Địa chỉ giao hàng tự động Cần Thơ',
                    N'Đơn hàng sinh tự động phục vụ Benchmark CT221H #' + CAST(N AS NVARCHAR(20)),
                    CASE N %% 4 
                        WHEN 0 THEN 'PAID'
                        WHEN 1 THEN 'DELIVERED'
                        WHEN 2 THEN 'PENDING'
                        ELSE 'CANCELLED' 
                    END AS TrangThai,
                    'BENCHMARK'
                FROM Numbers
                WHERE N <= %d;
                """, safeCount);

        long start = System.currentTimeMillis();
        jdbcTemplate.execute(sql);
        long elapsed = System.currentTimeMillis() - start;

        Long newTotal = jdbcTemplate.queryForObject("SELECT COUNT_BIG(1) FROM DonHang", Long.class);

        return Map.of(
                "status", "SUCCESS",
                "rowsInserted", safeCount,
                "executionTimeMs", elapsed,
                "totalOrdersNow", newTotal != null ? newTotal : safeCount,
                "message", "Đã sinh thành công " + safeCount + " bản ghi vào bảng DonHang bằng T-SQL CTE siêu tốc."
        );
    }

    @Override
    public Map<String, Object> toggleCoveringIndex(boolean enable) {
        try {
            if (enable) {
                ensureCoveringIndexExists();
                return Map.of("status", "SUCCESS", "indexName", "IX_DonHang_NgayTao_TrangThai_Covering", "state", "ENABLED");
            } else {
                jdbcTemplate.execute("DROP INDEX IF EXISTS IX_DonHang_NgayTao_TrangThai_Covering ON DonHang");
                return Map.of("status", "SUCCESS", "indexName", "IX_DonHang_NgayTao_TrangThai_Covering", "state", "DISABLED");
            }
        } catch (Exception e) {
            log.error("Lỗi thay đổi trạng thái Index: ", e);
            throw new com.ctu.freshgrocery.exception.AppException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Không thay đổi được chỉ mục. Kiểm tra quyền và nhật ký SQL Server.");
        }
    }

    private void ensureCoveringIndexExists() {
        String sql = """
                IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = N'IX_DonHang_NgayTao_TrangThai_Covering' AND object_id = OBJECT_ID('DonHang'))
                BEGIN
                    CREATE NONCLUSTERED INDEX IX_DonHang_NgayTao_TrangThai_Covering
                    ON DonHang (NgayTao, TrangThai)
                    INCLUDE (MaKH, TongTien, NguonDon);
                END
                """;
        jdbcTemplate.execute(sql);
    }
}
