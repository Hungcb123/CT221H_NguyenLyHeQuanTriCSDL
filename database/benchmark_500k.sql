USE FreshGroceryBenchmark500K;
GO
SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF DB_NAME() <> N'FreshGroceryBenchmark500K'
    THROW 51001, 'Run only on FreshGroceryBenchmark500K.', 1;
IF (SELECT COUNT_BIG(*) FROM dbo.DonHang) <> 500000
    THROW 51002, 'Expected exactly 500000 benchmark orders.', 1;

DECLARE @AsOf DATETIME2 = GETDATE();
DECLARE @Cutoff DATETIME2 = DATEADD(MONTH, -6, @AsOf);
SELECT DB_NAME() AS DatabaseName, SERVERPROPERTY('ProductVersion') AS SqlServerVersion,
       @AsOf AS AsOf, @Cutoff AS Cutoff;

-- One warm-up per query, then five measured runs per query with alternating order.
DECLARE @Step INT = 0;
WHILE @Step < 12
BEGIN
    IF @Step = 2
    BEGIN
        SET STATISTICS IO ON;
        SET STATISTICS TIME ON;
        SET STATISTICS XML ON;
    END;

    IF (@Step + @Step / 2) % 2 = 0
    BEGIN
        PRINT CONCAT('BENCHMARK|SCAN|', @Step);
        SELECT dh.MaKH, COUNT(dh.MaDH) AS SoDonHang, SUM(dh.TongTien) AS TongChiTieu
        FROM dbo.DonHang dh WITH (INDEX(0))
        WHERE dh.NgayTao >= @Cutoff AND dh.TrangThai IN ('PAID', 'DELIVERED')
        GROUP BY dh.MaKH
        ORDER BY dh.MaKH
        OPTION (RECOMPILE, MAXDOP 1);
    END
    ELSE
    BEGIN
        PRINT CONCAT('BENCHMARK|COVERING|', @Step);
        SELECT dh.MaKH, COUNT(dh.MaDH) AS SoDonHang, SUM(dh.TongTien) AS TongChiTieu
        FROM dbo.DonHang dh WITH (INDEX(IX_DonHang_NgayTao_TrangThai_Covering))
        WHERE dh.NgayTao >= @Cutoff AND dh.TrangThai IN ('PAID', 'DELIVERED')
        GROUP BY dh.MaKH
        ORDER BY dh.MaKH
        OPTION (RECOMPILE, MAXDOP 1);
    END;

    SET @Step += 1;
END;

SET STATISTICS XML OFF;
SET STATISTICS TIME OFF;
SET STATISTICS IO OFF;
