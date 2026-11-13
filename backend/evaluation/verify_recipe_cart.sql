USE FreshGroceryDB;
GO
SET NOCOUNT ON;
DECLARE @CountBefore INT = (SELECT COUNT(*) FROM ChiTietGioHang);
DECLARE @QtyBefore INT = (SELECT COALESCE(SUM(SoLuong), 0) FROM ChiTietGioHang);
DECLARE @Added INT, @Total DECIMAL(18,2), @Code INT, @Message NVARCHAR(255);
-- Make the demo ingredients expired for this test regardless of the seed date.
BEGIN TRANSACTION;
UPDATE SanPham SET HanSuDung = DATEADD(DAY, -1, GETDATE()) WHERE MaSP IN (1,3,4);
BEGIN TRY
    EXEC sp_ThemNguyenLieuMonAnVaoGioHang 1, 1, 4, @Added OUTPUT, @Total OUTPUT, @Code OUTPUT, @Message OUTPUT;
END TRY
BEGIN CATCH
    -- The procedure rolls back the enclosing fixture transaction on rejection.
    IF ERROR_NUMBER() <> 266 OR XACT_STATE() <> 0 THROW;
END CATCH;
IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
IF @Code IS NULL OR @Code >= 0 OR @Added IS NULL OR @Added <> 0
    THROW 51000, 'Expired ingredients were accepted', 1;
IF @CountBefore <> (SELECT COUNT(*) FROM ChiTietGioHang)
 OR @QtyBefore <> (SELECT COALESCE(SUM(SoLuong), 0) FROM ChiTietGioHang)
    THROW 51001, 'Cart changed after a failed recipe addition', 1;
PRINT 'Expired ingredients rejected without cart changes: PASS';

-- A temporary fresh batch must allow all three ingredients; roll it all back.
BEGIN TRANSACTION;
UPDATE SanPham SET HanSuDung = DATEADD(DAY, 2, GETDATE()) WHERE MaSP IN (1,3,4);
EXEC sp_ThemNguyenLieuMonAnVaoGioHang 3, 1, 4, @Added OUTPUT, @Total OUTPUT, @Code OUTPUT, @Message OUTPUT;
IF @Code <> 1 OR @Added <> 3
BEGIN
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW 51002, 'Fresh recipe addition failed', 1;
END;
ROLLBACK TRANSACTION;
PRINT 'Fresh recipe adds all ingredients; temporary test changes rolled back: PASS';
