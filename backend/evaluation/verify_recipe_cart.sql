USE FreshGroceryDB;
GO
SET NOCOUNT ON;
DECLARE @CountBefore INT = (SELECT COUNT(*) FROM ChiTietGioHang);
DECLARE @QtyBefore INT = (SELECT SUM(SoLuong) FROM ChiTietGioHang);
DECLARE @Added INT, @Total DECIMAL(18,2), @Code INT, @Message NVARCHAR(255);
-- Current demo ingredients are expired: reject without changing any cart.
EXEC sp_ThemNguyenLieuMonAnVaoGioHang 1, 1, 4, @Added OUTPUT, @Total OUTPUT, @Code OUTPUT, @Message OUTPUT;
IF @Code >= 0 OR @Added <> 0 THROW 51000, 'Expired ingredients were accepted', 1;
IF @CountBefore <> (SELECT COUNT(*) FROM ChiTietGioHang)
 OR @QtyBefore <> (SELECT SUM(SoLuong) FROM ChiTietGioHang)
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
