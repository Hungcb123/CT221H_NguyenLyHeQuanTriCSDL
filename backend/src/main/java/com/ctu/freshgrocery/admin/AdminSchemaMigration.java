package com.ctu.freshgrocery.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    @Override public void run(ApplicationArguments args) throws Exception {
        jdbc.execute(new org.springframework.core.io.ClassPathResource("product-types.sql")
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        jdbc.execute("""
                IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('DuBaoNhapHang')
                           AND name = 'DoTinCay' AND is_nullable = 0)
                    ALTER TABLE DuBaoNhapHang ALTER COLUMN DoTinCay DECIMAL(5,2) NULL;
                """);
        jdbc.execute("""
                IF NOT EXISTS (SELECT 1 FROM ChiTietDonHang)
                   AND EXISTS (SELECT 1 FROM SanPham WHERE TrangThaiXoa = 0)
                   AND EXISTS (SELECT 1 FROM KhachHang)
                BEGIN
                    DECLARE @DayIdx INT = 6;
                    WHILE @DayIdx >= 0
                    BEGIN
                        DECLARE @OrderDate DATETIME2 = DATEADD(HOUR, 10, CAST(DATEADD(DAY, -@DayIdx, CAST(GETDATE() AS DATE)) AS DATETIME2));
                        DECLARE @CustomerId INT = (SELECT TOP 1 MaKH FROM KhachHang ORDER BY MaKH);
                        DECLARE @Mult FLOAT = CASE @DayIdx
                            WHEN 6 THEN 0.92 WHEN 5 THEN 0.96 WHEN 4 THEN 1.00
                            WHEN 3 THEN 1.04 WHEN 2 THEN 1.08 WHEN 1 THEN 1.25 ELSE 1.12
                        END;
                        DECLARE @NewOrderId INT;
                        INSERT INTO DonHang (MaKH, NgayTao, TongTien, DiaChiGiaoHang, GhiChu, TrangThai, NguonDon)
                        VALUES (@CustomerId, @OrderDate, 0, N'Quận Ninh Kiều, TP. Cần Thơ', N'Đơn hàng lịch sử 7 ngày phục vụ thống kê & AI dự báo nhu cầu', 'DELIVERED', 'DEMO_FORECAST');
                        SET @NewOrderId = SCOPE_IDENTITY();
                        INSERT INTO ChiTietDonHang (MaDH, MaSP, SoLuong, DonGia, GiaGoc, TienGiam, MaKM)
                        SELECT @NewOrderId, sp.MaSP,
                            CAST(ROUND((CASE sp.MaSP
                                WHEN 1 THEN 45 WHEN 2 THEN 60 WHEN 3 THEN 85 WHEN 4 THEN 55
                                WHEN 5 THEN 70 WHEN 6 THEN 30 WHEN 7 THEN 40 WHEN 8 THEN 50
                                WHEN 9 THEN 18 WHEN 10 THEN 28 WHEN 11 THEN 42 WHEN 12 THEN 75 ELSE 30
                            END) * @Mult, 0) AS INT),
                            sp.Gia, sp.Gia, 0, NULL
                        FROM SanPham sp WHERE sp.TrangThaiXoa = 0;
                        UPDATE dh SET dh.TongTien = (SELECT ISNULL(SUM(ct.SoLuong * ct.DonGia), 0) FROM ChiTietDonHang ct WHERE ct.MaDH = @NewOrderId)
                        FROM DonHang dh WHERE dh.MaDH = @NewOrderId;
                        SET @DayIdx = @DayIdx - 1;
                    END
                END
                """);
    }
}
