USE FreshGroceryDB;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO
CREATE OR ALTER TRIGGER trg_SanPham_NganChanXoaVatLy
ON SanPham
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;

    -- Only the dedicated purge procedure enables physical deletion for this family on this connection.
    DECLARE @PurgeRoot INT = TRY_CONVERT(INT, SESSION_CONTEXT(N'DailyMartPurgeProductId'));
    IF @PurgeRoot IS NOT NULL
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM dbo.SanPham WHERE MaSP = @PurgeRoot AND MaSPCha IS NULL AND TrangThaiXoa = 1)
            THROW 50011, N'Chỉ được xóa vĩnh viễn sản phẩm đã xóa mềm.', 1;
        IF EXISTS (SELECT 1 FROM deleted WHERE MaSP <> @PurgeRoot AND ISNULL(MaSPCha, -1) <> @PurgeRoot)
            THROW 50011, N'Phạm vi xóa vĩnh viễn không hợp lệ.', 1;
        DELETE sp FROM dbo.SanPham sp JOIN deleted d ON d.MaSP = sp.MaSP;
        RETURN;
    END;

    -- 2.1. Tự động chuyển đổi hành vi thành cập nhật cờ xóa mềm TrangThaiXoa = 1
    UPDATE sp
    SET sp.TrangThaiXoa = 1,
        sp.NgayCapNhat = GETDATE()
    FROM SanPham sp
    INNER JOIN deleted d ON sp.MaSP = d.MaSP;

    -- 2.2. Ghi nhật ký truy vết hành động xóa vào bảng NhatKyCanhBao
    INSERT INTO NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP, NguoiThucHien, NgayGhi)
    SELECT
        'XOA_MEM_SAN_PHAM',
        N'[BẪY XÓA MỀM THÀNH CÔNG] Lệnh DELETE vật lý đã được chặn lại an toàn. Sản phẩm [' + d.TenSP +
        N'] (Mã SP: ' + CAST(d.MaSP AS NVARCHAR(10)) + N') được chuyển sang trạng thái Xóa Mềm (TrangThaiXoa = 1).',
        d.MaSP,
        SUSER_SNAME(),
        GETDATE()
    FROM deleted d;
END;
GO
CREATE OR ALTER PROCEDURE dbo.sp_XoaVinhVienSanPham
    @MaSP INT,
    @NguoiThucHien NVARCHAR(100) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    DECLARE @PreviousContext SQL_VARIANT = SESSION_CONTEXT(N'DailyMartPurgeProductId');
    DECLARE @Targets TABLE (MaSP INT PRIMARY KEY, TenSP NVARCHAR(200), DuLieu NVARCHAR(MAX));
    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @Deleted BIT, @Parent INT;
        SELECT @Deleted = TrangThaiXoa, @Parent = MaSPCha
        FROM dbo.SanPham WITH (UPDLOCK, HOLDLOCK) WHERE MaSP = @MaSP;
        IF @Deleted IS NULL THROW 50010, N'Không tìm thấy sản phẩm. Sản phẩm có thể đã được xóa vĩnh viễn.', 1;
        IF @Deleted = 0 THROW 50011, N'Hãy xóa mềm sản phẩm trước khi xóa vĩnh viễn.', 1;
        IF @Parent IS NOT NULL THROW 50011, N'Hãy xóa vĩnh viễn từ sản phẩm chính để xử lý các phân loại cùng nhau.', 1;

        INSERT INTO @Targets
        SELECT s.MaSP, s.TenSP, (SELECT s.* FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES)
        FROM dbo.SanPham s WITH (UPDLOCK, HOLDLOCK) WHERE s.MaSP = @MaSP OR s.MaSPCha = @MaSP;
        IF EXISTS (SELECT 1 FROM dbo.ChiTietDonHang c WITH (UPDLOCK, HOLDLOCK) JOIN @Targets t ON t.MaSP = c.MaSP)
            THROW 50012, N'Không thể xóa vĩnh viễn: sản phẩm hoặc phân loại đã có trong đơn hàng. Hãy giữ xóa mềm để bảo toàn lịch sử.', 1;
        IF EXISTS (SELECT 1 FROM dbo.DuBaoNhapHang d WITH (UPDLOCK, HOLDLOCK) JOIN @Targets t ON t.MaSP = d.MaSP)
            THROW 50013, N'Không thể xóa vĩnh viễn: sản phẩm hoặc phân loại còn lịch sử dự báo.', 1;
        IF EXISTS (SELECT 1 FROM dbo.NguyenLieuMonAn n WITH (UPDLOCK, HOLDLOCK) JOIN @Targets t ON t.MaSP = n.MaSP)
            THROW 50014, N'Không thể xóa vĩnh viễn: sản phẩm hoặc phân loại đang được dùng trong công thức món ăn.', 1;

        DELETE c FROM dbo.ChiTietGioHang c JOIN @Targets t ON t.MaSP = c.MaSP;
        DELETE c FROM dbo.ChiTietKhuyenMai c JOIN @Targets t ON t.MaSP = c.MaSP;
        EXEC sys.sp_set_session_context @key = N'DailyMartPurgeProductId', @value = @MaSP;
        DELETE s FROM dbo.SanPham s JOIN @Targets t ON t.MaSP = s.MaSP WHERE s.MaSP <> @MaSP;
        DELETE FROM dbo.SanPham WHERE MaSP = @MaSP;
        IF EXISTS (SELECT 1 FROM dbo.SanPham s JOIN @Targets t ON t.MaSP = s.MaSP)
            THROW 50015, N'Chưa xóa được toàn bộ sản phẩm và phân loại. Thao tác đã được hoàn tác.', 1;
        EXEC sys.sp_set_session_context @key = N'DailyMartPurgeProductId', @value = @PreviousContext;

        INSERT INTO dbo.NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP, NguoiThucHien)
        SELECT 'XOA_VINH_VIEN_SAN_PHAM', CONCAT(N'Đã xóa vĩnh viễn sản phẩm #', MaSP, N' ', TenSP, N'. Dữ liệu trước khi xóa: ', DuLieu),
               MaSP, COALESCE(NULLIF(LTRIM(RTRIM(@NguoiThucHien)), N''), SUSER_SNAME())
        FROM @Targets;
        COMMIT TRANSACTION;
        SELECT MaSP FROM @Targets ORDER BY MaSP;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        EXEC sys.sp_set_session_context @key = N'DailyMartPurgeProductId', @value = @PreviousContext;
        THROW;
    END CATCH;
END;
GO
