-- Types belong directly to an existing product. Retire the previous group association.
DECLARE @Migrate BIT = CASE WHEN COL_LENGTH(N'dbo.SanPham', N'MaSPCha') IS NULL THEN 1 ELSE 0 END;
IF @Migrate = 1
    ALTER TABLE dbo.SanPham ADD MaSPCha INT NULL CONSTRAINT FK_SanPham_Cha REFERENCES dbo.SanPham(MaSP);
IF COL_LENGTH(N'dbo.SanPham', N'TenLoai') IS NULL
    ALTER TABLE dbo.SanPham ADD TenLoai NVARCHAR(80) NULL;
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_SanPham_Loai')
    ALTER TABLE dbo.SanPham DROP CONSTRAINT CK_SanPham_Loai;
IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.SanPham') AND name = N'UX_SanPham_Loai')
    DROP INDEX UX_SanPham_Loai ON dbo.SanPham;
IF @Migrate = 1 AND COL_LENGTH(N'dbo.SanPham', N'MaNhomSP') IS NOT NULL
    EXEC(N'UPDATE dbo.SanPham SET MaNhomSP = NULL, TenLoai = NULL WHERE MaNhomSP IS NOT NULL');
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.SanPham') AND name = N'UX_SanPham_ChildType')
    EXEC(N'CREATE UNIQUE INDEX UX_SanPham_ChildType ON dbo.SanPham(MaSPCha, TenLoai) WHERE MaSPCha IS NOT NULL AND TrangThaiXoa = 0');
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_SanPham_ChildType')
    EXEC(N'ALTER TABLE dbo.SanPham ADD CONSTRAINT CK_SanPham_ChildType CHECK (MaSPCha IS NULL OR (MaSPCha <> MaSP AND TenLoai IS NOT NULL AND LEN(LTRIM(RTRIM(TenLoai))) > 0))');
DECLARE @Definition NVARCHAR(MAX) = OBJECT_DEFINITION(OBJECT_ID(N'dbo.sp_LayDanhSachSanPhamPhanTrang'));
IF @Definition IS NOT NULL AND @Definition NOT LIKE N'%sp.MaSPCha IS NULL%'
BEGIN
    SET @Definition = REPLACE(@Definition, N'CREATE OR ALTER PROCEDURE', N'ALTER PROCEDURE');
    SET @Definition = REPLACE(@Definition, N'CREATE   PROCEDURE', N'ALTER PROCEDURE');
    SET @Definition = REPLACE(@Definition, N'CREATE PROCEDURE', N'ALTER PROCEDURE');
    SET @Definition = REPLACE(@Definition, N'WHERE sp.TrangThaiXoa = 0', N'WHERE sp.TrangThaiXoa = 0 AND sp.MaSPCha IS NULL');
    EXEC sp_executesql @Definition;
END;

-- A family remains a family when all its types are hidden. Parent visibility gates sales.
DECLARE @TypeProcedure SYSNAME, @TypeDefinition NVARCHAR(MAX);
DECLARE TypeSaleProcedures CURSOR LOCAL FAST_FORWARD FOR
    SELECT name FROM sys.procedures WHERE name IN (N'sp_ThemVaoGioHang', N'sp_LayGioHangKhachHang', N'sp_ThanhToanDonHang', N'sp_ThemNguyenLieuMonAnVaoGioHang');
OPEN TypeSaleProcedures;
FETCH NEXT FROM TypeSaleProcedures INTO @TypeProcedure;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @TypeDefinition = OBJECT_DEFINITION(OBJECT_ID(N'dbo.' + QUOTENAME(@TypeProcedure)));
    IF @TypeDefinition IS NOT NULL AND @TypeDefinition NOT LIKE N'%child.MaSPCha = sp.MaSP%'
    BEGIN
        SET @TypeDefinition = REPLACE(@TypeDefinition, N'CREATE OR ALTER PROCEDURE', N'ALTER PROCEDURE');
        SET @TypeDefinition = REPLACE(@TypeDefinition, N'CREATE   PROCEDURE', N'ALTER PROCEDURE');
        SET @TypeDefinition = REPLACE(@TypeDefinition, N'CREATE PROCEDURE', N'ALTER PROCEDURE');
        SET @TypeDefinition = REPLACE(@TypeDefinition, N'IF EXISTS (SELECT 1 FROM SanPham WHERE MaSPCha = @MaSP AND TrangThaiXoa = 0)', N'IF EXISTS (SELECT 1 FROM SanPham WHERE MaSPCha = @MaSP)');
        SET @TypeDefinition = REPLACE(@TypeDefinition, N'OR EXISTS (SELECT 1 FROM SanPham parent WHERE parent.MaSP = sp.MaSPCha AND parent.TrangThaiXoa = 1)', N'OR EXISTS (SELECT 1 FROM SanPham parent WHERE parent.MaSP = sp.MaSPCha AND parent.TrangThaiXoa = 1)
                  OR EXISTS (SELECT 1 FROM SanPham child WHERE child.MaSPCha = sp.MaSP)');
        SET @TypeDefinition = REPLACE(@TypeDefinition, N'WHERE sp.SoLuongTon < item.SoLuongMua OR sp.HanSuDung <= GETDATE() OR sp.TrangThaiXoa = 1', N'WHERE sp.SoLuongTon < item.SoLuongMua OR sp.HanSuDung <= GETDATE() OR sp.TrangThaiXoa = 1
               OR EXISTS (SELECT 1 FROM SanPham parent WHERE parent.MaSP = sp.MaSPCha AND parent.TrangThaiXoa = 1)
               OR EXISTS (SELECT 1 FROM SanPham child WHERE child.MaSPCha = sp.MaSP)');
        SET @TypeDefinition = REPLACE(@TypeDefinition, N'AND (sp.MaSP IS NULL OR sp.TrangThaiXoa = 1 OR sp.HanSuDung <= GETDATE()', N'AND (sp.MaSP IS NULL OR sp.TrangThaiXoa = 1 OR sp.HanSuDung <= GETDATE()
                   OR EXISTS (SELECT 1 FROM SanPham parent WHERE parent.MaSP = sp.MaSPCha AND parent.TrangThaiXoa = 1)
                   OR EXISTS (SELECT 1 FROM SanPham child WHERE child.MaSPCha = sp.MaSP)');
        EXEC sp_executesql @TypeDefinition;
    END;
    FETCH NEXT FROM TypeSaleProcedures INTO @TypeProcedure;
END;
CLOSE TypeSaleProcedures;
DEALLOCATE TypeSaleProcedures;
