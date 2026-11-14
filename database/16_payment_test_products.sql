USE FreshGroceryDB;
GO
-- Sản phẩm demo để kiểm thử COD/SePay với số tiền nhỏ; chạy lại không nhân bản.
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @Category INT = (SELECT TOP 1 MaDanhMuc FROM DanhMuc ORDER BY MaDanhMuc);
IF @Category IS NULL THROW 50001, N'Cần tạo danh mục trước khi thêm sản phẩm test.', 1;
INSERT INTO SanPham (TenSP, MoTa, Gia, DonViTinh, SoLuongTon, HanSuDung, MaDanhMuc, HinhAnh)
SELECT v.TenSP, N'Sản phẩm DEMO kiểm thử thanh toán hệ thống; không giao hàng thực tế.', v.Gia,
       N'gói', 100, DATEADD(YEAR, 2, GETDATE()), @Category, N'/api/v1/product-images/demo-47.jpg'
FROM (VALUES (N'[TEST] Gói thử thanh toán 1.000đ', 1000),
             (N'[TEST] Gói thử thanh toán A 500đ', 500),
             (N'[TEST] Gói thử thanh toán B 500đ', 500)) v(TenSP, Gia)
WHERE NOT EXISTS (SELECT 1 FROM SanPham sp WITH (UPDLOCK, HOLDLOCK) WHERE sp.TenSP = v.TenSP);
COMMIT TRANSACTION;
GO
