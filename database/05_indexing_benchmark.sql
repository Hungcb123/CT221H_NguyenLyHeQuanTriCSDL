-- =========================================================================
-- ĐỒ ÁN CT221H: NGUYÊN LÝ HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU
-- PHÂN HỆ MODULE 5: ĐÁNH GIÁ HIỆU NĂNG CHỈ MỤC (INDEXING BENCHMARK 1.000.000 DÒNG)
-- FILE: 05_indexing_benchmark.sql
-- =========================================================================

USE FreshGroceryDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- 1. THỦ TỤC SINH DỮ LIỆU LỚN BẰNG KỸ THUẬT T-SQL CTE (CROSS JOIN SIÊU TỐC)
CREATE OR ALTER PROCEDURE sp_SinhDuLieuLonBenchmark
    @SoLuongDonHang INT = 500000,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        PRINT N'Đang bắt đầu sinh ' + FORMAT(@SoLuongDonHang, '#,##0') + N' đơn hàng mẫu...';

        -- Sinh CTE 1.000.000 số nguyên
        ;WITH 
          E1(N) AS (SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1 UNION ALL SELECT 1), -- 10
          E2(N) AS (SELECT 1 FROM E1 a CROSS JOIN E1 b), -- 100
          E3(N) AS (SELECT 1 FROM E2 a CROSS JOIN E2 b), -- 10.000
          E4(N) AS (SELECT 1 FROM E3 a CROSS JOIN E1 b), -- 100.000
          E5(N) AS (SELECT 1 FROM E4 a CROSS JOIN E1 b), -- 1.000.000
          Numbers(N) AS (SELECT ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) FROM E5)
        INSERT INTO DonHang (MaKH, NgayTao, TongTien, DiaChiGiaoHang, GhiChu, TrangThai)
        SELECT 
            (ABS(CHECKSUM(NEWID())) % 3) + 1 AS MaKH, -- Phân bổ cho 3 khách hàng mẫu
            DATEADD(MINUTE, - (N % 525600), GETDATE()) AS NgayTao, -- Trải dài trong 1 năm gần nhất
            ROUND(RAND(CHECKSUM(NEWID())) * (2000000 - 50000) + 50000, -3) AS TongTien,
            N'Địa chỉ giao hàng tự động Cần Thơ',
            N'Đơn hàng sinh tự động phục vụ Benchmark CT221H #' + CAST(N AS NVARCHAR(20)),
            CASE N % 4 
                WHEN 0 THEN 'PAID'
                WHEN 1 THEN 'DELIVERED'
                WHEN 2 THEN 'PENDING'
                ELSE 'CANCELLED' 
            END AS TrangThai
        FROM Numbers
        WHERE N <= @SoLuongDonHang;

        SET @StatusCode = 1;
        SET @Message = N'Đã sinh thành công ' + FORMAT(@SoLuongDonHang, '#,##0') + N' dòng dữ liệu DonHang vào CSDL.';
    END TRY
    BEGIN CATCH
        SET @StatusCode = -99;
        SET @Message = N'Lỗi sinh dữ liệu: ' + ERROR_MESSAGE();
    END CATCH
END;
GO

-- 2. TẠO CHỈ MỤC BAO PHỦ COVERING INDEX TRÊN DonHang
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = N'IX_DonHang_NgayTao_TrangThai_Covering' AND object_id = OBJECT_ID('DonHang'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_DonHang_NgayTao_TrangThai_Covering
    ON DonHang (NgayTao, TrangThai)
    INCLUDE (MaKH, TongTien);
    PRINT N'Đã tạo chỉ mục bao phủ: IX_DonHang_NgayTao_TrangThai_Covering';
END
GO

-- 3. TẠO CHỈ MỤC LỌC FILTERED INDEX TRÊN CÁC ĐƠN HÀNG THÀNH CÔNG
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = N'IX_DonHang_Filtered_Paid' AND object_id = OBJECT_ID('DonHang'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_DonHang_Filtered_Paid
    ON DonHang (NgayTao)
    INCLUDE (MaKH, TongTien)
    WHERE TrangThai IN ('PAID', 'DELIVERED');
    PRINT N'Đã tạo chỉ mục lọc: IX_DonHang_Filtered_Paid';
END
GO

-- 4. THỦ TỤC THỰC THI BÁO CÁO PHÂN TÍCH DOANH SỐ ĐO KIỂM HIỆU NĂNG
CREATE OR ALTER PROCEDURE sp_BenchmarkBaoCaoDoanhThu
    @UseIndex BIT = 1,
    @ExecutionTimeMs INT OUTPUT,
    @TotalRecords INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @StartTime DATETIME2 = SYSUTCDATETIME();

    IF (@UseIndex = 1)
    BEGIN
        -- Kịch bản có Index: Ép dùng Covering Index Seek
        SELECT 
            dh.MaKH,
            COUNT(dh.MaDH) AS SoDonHang,
            SUM(dh.TongTien) AS TongChiTieu
        FROM DonHang dh WITH (INDEX(IX_DonHang_NgayTao_TrangThai_Covering))
        WHERE dh.NgayTao >= DATEADD(MONTH, -6, GETDATE()) 
          AND dh.TrangThai IN ('PAID', 'DELIVERED')
        GROUP BY dh.MaKH;

        SET @TotalRecords = @@ROWCOUNT;
    END
    ELSE
    BEGIN
        -- Kịch bản KHÔNG Index: Ép Table Scan / Clustered Index Scan tuần tự
        SELECT 
            dh.MaKH,
            COUNT(dh.MaDH) AS SoDonHang,
            SUM(dh.TongTien) AS TongChiTieu
        FROM DonHang dh WITH (INDEX(0))
        WHERE dh.NgayTao >= DATEADD(MONTH, -6, GETDATE()) 
          AND dh.TrangThai IN ('PAID', 'DELIVERED')
        GROUP BY dh.MaKH;

        SET @TotalRecords = @@ROWCOUNT;
    END

    SET @ExecutionTimeMs = DATEDIFF(MILLISECOND, @StartTime, SYSUTCDATETIME());
END;
GO
