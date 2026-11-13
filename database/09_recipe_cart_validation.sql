USE FreshGroceryDB;
GO
CREATE OR ALTER PROCEDURE sp_ThemNguyenLieuMonAnVaoGioHang
    @MaKH INT,
    @MaCongThuc INT,
    @KhauPhan INT = NULL,
    @SoLuongMonDaThem INT OUTPUT,
    @TongTienNguyenLieu DECIMAL(18,2) OUTPUT,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM KhachHang WHERE MaKH = @MaKH)
        BEGIN
            SET @StatusCode = -1;
            SET @Message = N'Mã khách hàng không tồn tại.';
            SET @SoLuongMonDaThem = 0;
            SET @TongTienNguyenLieu = 0;
            RETURN;
        END

        IF NOT EXISTS (SELECT 1 FROM CongThucMonAn WHERE MaCongThuc = @MaCongThuc)
        BEGIN
            SET @StatusCode = -2;
            SET @Message = N'Công thức món ăn không tồn tại trong hệ thống.';
            SET @SoLuongMonDaThem = 0;
            SET @TongTienNguyenLieu = 0;
            RETURN;
        END

        DECLARE @KhauPhanChuan INT;
        SELECT @KhauPhanChuan = KhauPhanMacDinh FROM CongThucMonAn WHERE MaCongThuc = @MaCongThuc;
        IF (@KhauPhan IS NULL OR @KhauPhan <= 0) SET @KhauPhan = @KhauPhanChuan;

        -- Tỷ lệ nhân khẩu phần
        DECLARE @TyLe DECIMAL(10,2) = CAST(@KhauPhan AS DECIMAL(10,2)) / CAST(@KhauPhanChuan AS DECIMAL(10,2));

        -- Đảm bảo khách hàng đã có giỏ hàng
        BEGIN TRANSACTION;
        -- Serialize recipe additions for one customer, including empty carts.
        DECLARE @LockedCustomer INT;
        SELECT @LockedCustomer = MaKH FROM KhachHang WITH (UPDLOCK, HOLDLOCK) WHERE MaKH = @MaKH;
        IF NOT EXISTS (SELECT 1 FROM NguyenLieuMonAn WHERE MaCongThuc = @MaCongThuc)
            THROW 50001, N'Công thức chưa có nguyên liệu.', 1;
        IF EXISTS (
            SELECT 1 FROM NguyenLieuMonAn nl
            LEFT JOIN SanPham sp WITH (UPDLOCK, HOLDLOCK) ON sp.MaSP = nl.MaSP
            LEFT JOIN GioHang gh ON gh.MaKH = @MaKH
            LEFT JOIN ChiTietGioHang ct ON ct.MaGioHang = gh.MaGioHang AND ct.MaSP = nl.MaSP
            WHERE nl.MaCongThuc = @MaCongThuc
              AND (sp.MaSP IS NULL OR sp.TrangThaiXoa = 1 OR sp.HanSuDung <= GETDATE()
                   OR sp.SoLuongTon < CEILING(nl.SoLuongDinhLuong * @TyLe) + ISNULL(ct.SoLuong, 0))
        )
            THROW 50002, N'Nguyên liệu đã hết hạn, ngừng kinh doanh hoặc không đủ số lượng (kể cả trong giỏ).', 1;

        DECLARE @MaGioHang INT;
        SELECT @MaGioHang = MaGioHang FROM GioHang WHERE MaKH = @MaKH;
        IF (@MaGioHang IS NULL)
        BEGIN
            INSERT INTO GioHang (MaKH, NgayCapNhat) VALUES (@MaKH, GETDATE());
            SET @MaGioHang = SCOPE_IDENTITY();
        END

        -- Bảng tạm tính toán nguyên liệu
        DECLARE @NguyenLieu TABLE (
            MaSP INT,
            SoLuongCanMua INT,
            Gia DECIMAL(18,2)
        );

        INSERT INTO @NguyenLieu (MaSP, SoLuongCanMua, Gia)
        SELECT 
            nl.MaSP,
            CAST(CEILING(nl.SoLuongDinhLuong * @TyLe) AS INT),
            sp.Gia
        FROM NguyenLieuMonAn nl
        INNER JOIN SanPham sp ON nl.MaSP = sp.MaSP
        WHERE nl.MaCongThuc = @MaCongThuc AND sp.TrangThaiXoa = 0;

        -- Thêm hoặc cộng dồn vào ChiTietGioHang
        MERGE ChiTietGioHang AS target
        USING @NguyenLieu AS source
        ON (target.MaGioHang = @MaGioHang AND target.MaSP = source.MaSP)
        WHEN MATCHED THEN
            UPDATE SET target.SoLuong = target.SoLuong + source.SoLuongCanMua
        WHEN NOT MATCHED THEN
            INSERT (MaGioHang, MaSP, SoLuong) VALUES (@MaGioHang, source.MaSP, source.SoLuongCanMua);

        UPDATE GioHang SET NgayCapNhat = GETDATE() WHERE MaGioHang = @MaGioHang;

        SELECT 
            @SoLuongMonDaThem = COUNT(1),
            @TongTienNguyenLieu = ISNULL(SUM(SoLuongCanMua * Gia), 0)
        FROM @NguyenLieu;

        COMMIT TRANSACTION;
        SET @StatusCode = 1;
        SET @Message = N'Đã thêm ' + CAST(@SoLuongMonDaThem AS NVARCHAR(10)) + N' nguyên liệu món ăn vào giỏ hàng thành công.';
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        SET @StatusCode = -99;
        SET @Message = N'Lỗi thêm nguyên liệu món ăn: ' + ERROR_MESSAGE();
        SET @SoLuongMonDaThem = 0;
        SET @TongTienNguyenLieu = 0;
    END CATCH
END;
GO
