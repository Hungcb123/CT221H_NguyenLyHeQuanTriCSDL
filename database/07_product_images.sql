USE FreshGroceryDB;
GO
IF COL_LENGTH('dbo.SanPham', 'HinhAnh') IS NULL
    ALTER TABLE dbo.SanPham ADD HinhAnh NVARCHAR(1000) NULL;
GO
