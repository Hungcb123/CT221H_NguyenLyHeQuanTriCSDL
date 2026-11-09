-- =========================================================================
-- ĐỒ ÁN CT221H: NGUYÊN LÝ HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU
-- FILE: 02_stored_procedures.sql - Các thủ tục lưu trữ CRUD Sản Phẩm
-- =========================================================================

USE FreshGroceryDB;
GO

-- 1. THỦ TỤC THÊM MỚI SẢN PHẨM TƯƠI SỐNG (REQ-SP-01)
CREATE OR ALTER PROCEDURE sp_ThemSanPhamMoi
    @TenSP NVARCHAR(200),
    @MoTa NVARCHAR(MAX),
    @Gia DECIMAL(18,2),
    @DonViTinh NVARCHAR(20),
    @SoLuongTon INT,
    @HanSuDung DATETIME2,
    @NhietDoBaoQuan NVARCHAR(50),
    @MaDanhMuc INT,
    @NewMaSP INT OUTPUT,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT,
    @HinhAnh NVARCHAR(1000) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        -- Kiểm tra giá hợp lệ
        IF (@Gia IS NULL OR @Gia <= 0)
        BEGIN
            SET @NewMaSP = 0;
            SET @StatusCode = -1;
            SET @Message = N'Giá bán sản phẩm phải lớn hơn 0 VND.';
            RETURN;
        END

        -- Kiểm tra số lượng tồn kho
        IF (@SoLuongTon IS NULL OR @SoLuongTon < 0)
        BEGIN
            SET @NewMaSP = 0;
            SET @StatusCode = -2;
            SET @Message = N'Số lượng tồn kho không được là số âm.';
            RETURN;
        END

        -- Kiểm tra hạn sử dụng của thực phẩm tươi
        IF (@HanSuDung IS NULL OR @HanSuDung <= GETDATE())
        BEGIN
            SET @NewMaSP = 0;
            SET @StatusCode = -3;
            SET @Message = N'Hạn sử dụng thực phẩm tươi sống phải lớn hơn thời điểm hiện tại.';
            RETURN;
        END

        -- Kiểm tra danh mục tồn tại
        IF NOT EXISTS (SELECT 1 FROM DanhMuc WHERE MaDanhMuc = @MaDanhMuc)
        BEGIN
            SET @NewMaSP = 0;
            SET @StatusCode = -4;
            SET @Message = N'Mã danh mục không tồn tại trong hệ thống.';
            RETURN;
        END

        -- Kiểm tra tên sản phẩm
        IF (@TenSP IS NULL OR LTRIM(RTRIM(@TenSP)) = N'')
        BEGIN
            SET @NewMaSP = 0;
            SET @StatusCode = -5;
            SET @Message = N'Tên sản phẩm không được để trống.';
            RETURN;
        END

        -- Thêm mới sản phẩm
        INSERT INTO SanPham (
            TenSP, MoTa, HinhAnh, Gia, DonViTinh, SoLuongTon, 
            HanSuDung, NhietDoBaoQuan, MaDanhMuc, TrangThaiXoa, NgayTao, NgayCapNhat
        )
        VALUES (
            @TenSP, @MoTa, @HinhAnh, @Gia, @DonViTinh, @SoLuongTon, 
            @HanSuDung, @NhietDoBaoQuan, @MaDanhMuc, 0, GETDATE(), GETDATE()
        );

        SET @NewMaSP = SCOPE_IDENTITY();
        SET @StatusCode = 1;
        SET @Message = N'Thêm mới sản phẩm tươi sống thành công.';
    END TRY
    BEGIN CATCH
        SET @NewMaSP = 0;
        SET @StatusCode = -99;
        SET @Message = N'Lỗi hệ thống: ' + ERROR_MESSAGE();
    END CATCH
END;
GO

-- 2. THỦ TỤC CẬP NHẬT THÔNG TIN SẢN PHẨM (REQ-SP-02)
CREATE OR ALTER PROCEDURE sp_CapNhatSanPham
    @MaSP INT,
    @TenSP NVARCHAR(200),
    @MoTa NVARCHAR(MAX),
    @Gia DECIMAL(18,2),
    @DonViTinh NVARCHAR(20),
    @SoLuongTon INT,
    @HanSuDung DATETIME2,
    @NhietDoBaoQuan NVARCHAR(50),
    @MaDanhMuc INT,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT,
    @HinhAnh NVARCHAR(1000) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        -- Kiểm tra sản phẩm có tồn tại và chưa bị xóa mềm không
        IF NOT EXISTS (SELECT 1 FROM SanPham WHERE MaSP = @MaSP AND TrangThaiXoa = 0)
        BEGIN
            SET @StatusCode = -1;
            SET @Message = N'Sản phẩm không tồn tại hoặc đã bị xóa mềm.';
            RETURN;
        END

        -- Kiểm tra tính hợp lệ giá
        IF (@Gia IS NOT NULL AND @Gia <= 0)
        BEGIN
            SET @StatusCode = -2;
            SET @Message = N'Giá bán phải lớn hơn 0 VND.';
            RETURN;
        END

        -- Kiểm tra số lượng tồn
        IF (@SoLuongTon IS NOT NULL AND @SoLuongTon < 0)
        BEGIN
            SET @StatusCode = -3;
            SET @Message = N'Số lượng tồn kho không được âm.';
            RETURN;
        END

        -- Kiểm tra danh mục
        IF (@MaDanhMuc IS NOT NULL AND NOT EXISTS (SELECT 1 FROM DanhMuc WHERE MaDanhMuc = @MaDanhMuc))
        BEGIN
            SET @StatusCode = -4;
            SET @Message = N'Mã danh mục không hợp lệ.';
            RETURN;
        END

        -- Thực hiện cập nhật (Lệnh UPDATE này sẽ kích hoạt trigger trg_SanPham_CanhBaoBienDong nếu có giá bất thường hoặc hết hàng)
        UPDATE SanPham
        SET 
            TenSP = ISNULL(@TenSP, TenSP),
            MoTa = ISNULL(@MoTa, MoTa),
            HinhAnh = ISNULL(@HinhAnh, HinhAnh),
            Gia = ISNULL(@Gia, Gia),
            DonViTinh = ISNULL(@DonViTinh, DonViTinh),
            SoLuongTon = ISNULL(@SoLuongTon, SoLuongTon),
            HanSuDung = ISNULL(@HanSuDung, HanSuDung),
            NhietDoBaoQuan = ISNULL(@NhietDoBaoQuan, NhietDoBaoQuan),
            MaDanhMuc = ISNULL(@MaDanhMuc, MaDanhMuc),
            NgayCapNhat = GETDATE()
        WHERE MaSP = @MaSP;

        SET @StatusCode = 1;
        SET @Message = N'Cập nhật thông tin sản phẩm thành công.';
    END TRY
    BEGIN CATCH
        SET @StatusCode = -99;
        SET @Message = N'Lỗi hệ thống: ' + ERROR_MESSAGE();
    END CATCH
END;
GO

-- 3. THỦ TỤC TRUY VẤN DANH SÁCH SẢN PHẨM PHÂN TRANG & LỌC (REQ-SP-03)
CREATE OR ALTER PROCEDURE sp_LayDanhSachSanPhamPhanTrang
    @MaDanhMuc INT = NULL,
    @TuKhoa NVARCHAR(100) = NULL,
    @GiaTu DECIMAL(18,2) = NULL,
    @GiaDen DECIMAL(18,2) = NULL,
    @SapXep VARCHAR(20) = 'MOI_NHAT', -- MOI_NHAT, GIA_ASC, GIA_DESC, HSD_ASC, TON_DESC
    @TrangHienTai INT = 1,
    @SoDongTrenTrang INT = 10,
    @TongSoBanGhi INT OUTPUT,
    @TongSoTrang INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    -- Chuẩn hóa tham số phân trang
    IF (@TrangHienTai IS NULL OR @TrangHienTai < 1) SET @TrangHienTai = 1;
    IF (@SoDongTrenTrang IS NULL OR @SoDongTrenTrang < 1) SET @SoDongTrenTrang = 10;
    IF (@TuKhoa IS NOT NULL) SET @TuKhoa = LTRIM(RTRIM(@TuKhoa));

    -- 1. Tính tổng số bản ghi thỏa mãn điều kiện lọc
    SELECT @TongSoBanGhi = COUNT(1)
    FROM SanPham sp
    INNER JOIN DanhMuc dm ON sp.MaDanhMuc = dm.MaDanhMuc
    OUTER APPLY (
        SELECT TOP 1 
            ctkm.MaKM,
            ctkm.GiaKhuyenMai,
            ctkm.PhanTramGiam,
            km.TenKM
        FROM ChiTietKhuyenMai ctkm
        INNER JOIN ChuongTrinhKhuyenMai km ON ctkm.MaKM = km.MaKM
        WHERE ctkm.MaSP = sp.MaSP
          AND km.TrangThai = 1
          AND GETDATE() BETWEEN km.NgayBatDau AND km.NgayKetThuc
          AND (ctkm.SoLuongKhuyenMai IS NULL OR ctkm.SoLuongDaBan < ctkm.SoLuongKhuyenMai)
        ORDER BY ctkm.GiaKhuyenMai ASC
    ) km_active
    WHERE sp.TrangThaiXoa = 0
      AND (@MaDanhMuc IS NULL OR sp.MaDanhMuc = @MaDanhMuc)
      AND (@TuKhoa IS NULL OR sp.TenSP LIKE N'%' + @TuKhoa + N'%' OR sp.MoTa LIKE N'%' + @TuKhoa + N'%')
      AND (@GiaTu IS NULL OR ISNULL(km_active.GiaKhuyenMai, sp.Gia) >= @GiaTu)
      AND (@GiaDen IS NULL OR ISNULL(km_active.GiaKhuyenMai, sp.Gia) <= @GiaDen);

    -- 2. Tính tổng số trang
    SET @TongSoTrang = CEILING(CAST(@TongSoBanGhi AS FLOAT) / CAST(@SoDongTrenTrang AS FLOAT));
    IF (@TongSoTrang = 0) SET @TongSoTrang = 1;

    -- 3. Trả về tập kết quả (ResultSet) sử dụng OFFSET ... FETCH NEXT kèm thông tin khuyến mãi
    SELECT 
        sp.MaSP,
        sp.TenSP,
        sp.MoTa,
        sp.HinhAnh,
        sp.Gia AS GiaGoc,
        ISNULL(km_active.GiaKhuyenMai, sp.Gia) AS GiaHienTai,
        CASE WHEN km_active.GiaKhuyenMai IS NOT NULL THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END AS DangKhuyenMai,
        km_active.PhanTramGiam,
        km_active.TenKM AS TenChuongTrinhKM,
        km_active.MaKM,
        sp.DonViTinh,
        sp.SoLuongTon,
        sp.HanSuDung,
        sp.NhietDoBaoQuan,
        sp.MaDanhMuc,
        dm.TenDanhMuc,
        sp.NgayTao,
        sp.NgayCapNhat
    FROM SanPham sp
    INNER JOIN DanhMuc dm ON sp.MaDanhMuc = dm.MaDanhMuc
    OUTER APPLY (
        SELECT TOP 1 
            ctkm.MaKM,
            ctkm.GiaKhuyenMai,
            ctkm.PhanTramGiam,
            km.TenKM
        FROM ChiTietKhuyenMai ctkm
        INNER JOIN ChuongTrinhKhuyenMai km ON ctkm.MaKM = km.MaKM
        WHERE ctkm.MaSP = sp.MaSP
          AND km.TrangThai = 1
          AND GETDATE() BETWEEN km.NgayBatDau AND km.NgayKetThuc
          AND (ctkm.SoLuongKhuyenMai IS NULL OR ctkm.SoLuongDaBan < ctkm.SoLuongKhuyenMai)
        ORDER BY ctkm.GiaKhuyenMai ASC
    ) km_active
    WHERE sp.TrangThaiXoa = 0
      AND (@MaDanhMuc IS NULL OR sp.MaDanhMuc = @MaDanhMuc)
      AND (@TuKhoa IS NULL OR sp.TenSP LIKE N'%' + @TuKhoa + N'%' OR sp.MoTa LIKE N'%' + @TuKhoa + N'%')
      AND (@GiaTu IS NULL OR ISNULL(km_active.GiaKhuyenMai, sp.Gia) >= @GiaTu)
      AND (@GiaDen IS NULL OR ISNULL(km_active.GiaKhuyenMai, sp.Gia) <= @GiaDen)
    ORDER BY 
        CASE WHEN @SapXep = 'GIA_ASC' THEN ISNULL(km_active.GiaKhuyenMai, sp.Gia) END ASC,
        CASE WHEN @SapXep = 'GIA_DESC' THEN ISNULL(km_active.GiaKhuyenMai, sp.Gia) END DESC,
        CASE WHEN @SapXep = 'HSD_ASC' THEN sp.HanSuDung END ASC,
        CASE WHEN @SapXep = 'TON_DESC' THEN sp.SoLuongTon END DESC,
        CASE WHEN @SapXep = 'MOI_NHAT' OR @SapXep IS NULL THEN sp.MaSP END DESC
    OFFSET (@TrangHienTai - 1) * @SoDongTrenTrang ROWS
    FETCH NEXT @SoDongTrenTrang ROWS ONLY;
END;
GO

-- 4. THỦ TỤC LẤY CHI TIẾT SẢN PHẨM THEO MÃ
CREATE OR ALTER PROCEDURE sp_LayChiTietSanPham
    @MaSP INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT 
        sp.MaSP,
        sp.TenSP,
        sp.MoTa,
        sp.HinhAnh,
        sp.Gia AS GiaGoc,
        ISNULL(km_active.GiaKhuyenMai, sp.Gia) AS GiaHienTai,
        CASE WHEN km_active.GiaKhuyenMai IS NOT NULL THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END AS DangKhuyenMai,
        km_active.PhanTramGiam,
        km_active.TenKM AS TenChuongTrinhKM,
        km_active.MaKM,
        sp.DonViTinh,
        sp.SoLuongTon,
        sp.HanSuDung,
        sp.NhietDoBaoQuan,
        sp.MaDanhMuc,
        dm.TenDanhMuc,
        sp.TrangThaiXoa,
        sp.NgayTao,
        sp.NgayCapNhat
    FROM SanPham sp
    INNER JOIN DanhMuc dm ON sp.MaDanhMuc = dm.MaDanhMuc
    OUTER APPLY (
        SELECT TOP 1 
            ctkm.MaKM,
            ctkm.GiaKhuyenMai,
            ctkm.PhanTramGiam,
            km.TenKM
        FROM ChiTietKhuyenMai ctkm
        INNER JOIN ChuongTrinhKhuyenMai km ON ctkm.MaKM = km.MaKM
        WHERE ctkm.MaSP = sp.MaSP
          AND km.TrangThai = 1
          AND GETDATE() BETWEEN km.NgayBatDau AND km.NgayKetThuc
          AND (ctkm.SoLuongKhuyenMai IS NULL OR ctkm.SoLuongDaBan < ctkm.SoLuongKhuyenMai)
        ORDER BY ctkm.GiaKhuyenMai ASC
    ) km_active
    WHERE sp.MaSP = @MaSP;
END;
GO

-- =========================================================================
-- PHÂN HỆ MODULE 2: GIAO DỊCH VÍ ĐIỆN TỬ, GIỎ HÀNG & THANH TOÁN ĐƠN HÀNG
-- =========================================================================

-- 5. THỦ TỤC NẠP TIỀN VÀO VÍ ĐIỆN TỬ (TRANSACTION ACID & SỔ CÁI KIỂM TOÁN)
CREATE OR ALTER PROCEDURE sp_NapTienVi
    @MaKH INT,
    @SoTien DECIMAL(18,2),
    @NoiDung NVARCHAR(255),
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT,
    @SoDuMoi DECIMAL(18,2) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @SoDuTruoc DECIMAL(18,2) = 0;

    BEGIN TRY
        IF (@SoTien IS NULL OR @SoTien <= 0)
        BEGIN
            SET @StatusCode = -1;
            SET @Message = N'Số tiền nạp vào ví phải lớn hơn 0 VND.';
            SET @SoDuMoi = 0;
            RETURN;
        END

        BEGIN TRANSACTION;

        -- Khóa dòng độc quyền trên KhachHang (UPDLOCK, ROWLOCK)
        SELECT @SoDuTruoc = SoDuVi
        FROM KhachHang WITH (UPDLOCK, ROWLOCK)
        WHERE MaKH = @MaKH;

        IF (@SoDuTruoc IS NULL)
        BEGIN
            SET @StatusCode = -2;
            SET @Message = N'Mã khách hàng không tồn tại trong hệ thống.';
            SET @SoDuMoi = 0;
            ROLLBACK TRANSACTION;
            RETURN;
        END

        SET @SoDuMoi = @SoDuTruoc + @SoTien;

        -- 1. Cập nhật số dư ví
        UPDATE KhachHang
        SET SoDuVi = @SoDuMoi
        WHERE MaKH = @MaKH;

        -- 2. Ghi sổ cái kiểm toán giao dịch (Audit Trail)
        INSERT INTO LichSuGiaoDichVi (MaKH, MaDH, LoaiGiaoDich, SoDuTruoc, SoTienThayDoi, SoDuSau, NgayGiaoDich, NoiDung)
        VALUES (
            @MaKH, 
            NULL, 
            'NAP_TIEN', 
            @SoDuTruoc, 
            @SoTien, 
            @SoDuMoi, 
            GETDATE(), 
            ISNULL(@NoiDung, N'Nạp tiền vào ví điện tử Bách Hóa Xanh')
        );

        COMMIT TRANSACTION;

        SET @StatusCode = 1;
        SET @Message = N'Nạp tiền vào ví thành công. Số dư mới: ' + FORMAT(@SoDuMoi, '#,##0') + N' VND.';
    END TRY
    BEGIN CATCH
        IF (XACT_STATE() <> 0) ROLLBACK TRANSACTION;
        SET @StatusCode = -99;
        SET @Message = N'Lỗi nạp tiền ví: ' + ERROR_MESSAGE();
        SET @SoDuMoi = 0;
    END CATCH
END;
GO

-- 6. THỦ TỤC THÊM SẢN PHẨM VÀO GIỎ HÀNG
CREATE OR ALTER PROCEDURE sp_ThemVaoGioHang
    @MaKH INT,
    @MaSP INT,
    @SoLuong INT,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        IF (@SoLuong IS NULL OR @SoLuong <= 0)
        BEGIN
            SET @StatusCode = -1;
            SET @Message = N'Số lượng thêm vào giỏ phải lớn hơn 0.';
            RETURN;
        END

        -- Kiểm tra sản phẩm tồn tại, còn hàng và chưa hết hạn
        IF NOT EXISTS (SELECT 1 FROM SanPham WHERE MaSP = @MaSP AND TrangThaiXoa = 0 AND HanSuDung > GETDATE())
        BEGIN
            SET @StatusCode = -2;
            SET @Message = N'Sản phẩm không tồn tại, đã hết hạn hoặc tạm ngừng kinh doanh.';
            RETURN;
        END

        -- Đảm bảo khách hàng đã có giỏ hàng (nếu chưa có thì tự tạo)
        DECLARE @MaGioHang INT;
        SELECT @MaGioHang = MaGioHang FROM GioHang WHERE MaKH = @MaKH;
        IF (@MaGioHang IS NULL)
        BEGIN
            INSERT INTO GioHang (MaKH, NgayCapNhat) VALUES (@MaKH, GETDATE());
            SET @MaGioHang = SCOPE_IDENTITY();
        END

        -- Thêm hoặc cộng dồn số lượng vào ChiTietGioHang
        MERGE ChiTietGioHang AS target
        USING (SELECT @MaGioHang AS MaGioHang, @MaSP AS MaSP, @SoLuong AS SoLuong) AS source
        ON (target.MaGioHang = source.MaGioHang AND target.MaSP = source.MaSP)
        WHEN MATCHED THEN
            UPDATE SET target.SoLuong = target.SoLuong + source.SoLuong
        WHEN NOT MATCHED THEN
            INSERT (MaGioHang, MaSP, SoLuong) VALUES (source.MaGioHang, source.MaSP, source.SoLuong);

        UPDATE GioHang SET NgayCapNhat = GETDATE() WHERE MaGioHang = @MaGioHang;

        SET @StatusCode = 1;
        SET @Message = N'Đã thêm sản phẩm vào giỏ hàng thành công.';
    END TRY
    BEGIN CATCH
        SET @StatusCode = -99;
        SET @Message = ERROR_MESSAGE();
    END CATCH
END;
GO

-- 7. THỦ TỤC XÓA SẢN PHẨM KHỎI GIỎ HÀNG
CREATE OR ALTER PROCEDURE sp_XoaKhoiGioHang
    @MaKH INT,
    @MaSP INT,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        DECLARE @MaGioHang INT;
        SELECT @MaGioHang = MaGioHang FROM GioHang WHERE MaKH = @MaKH;

        IF (@MaGioHang IS NULL)
        BEGIN
            SET @StatusCode = -1;
            SET @Message = N'Khách hàng chưa có giỏ hàng.';
            RETURN;
        END

        DELETE FROM ChiTietGioHang WHERE MaGioHang = @MaGioHang AND MaSP = @MaSP;

        SET @StatusCode = 1;
        SET @Message = N'Đã xóa sản phẩm khỏi giỏ hàng.';
    END TRY
    BEGIN CATCH
        SET @StatusCode = -99;
        SET @Message = ERROR_MESSAGE();
    END CATCH
END;
GO

-- 8. THỦ TỤC LẤY CHI TIẾT GIỎ HÀNG CỦA KHÁCH HÀNG
CREATE OR ALTER PROCEDURE sp_LayGioHangKhachHang
    @MaKH INT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @MaGioHang INT;
    SELECT @MaGioHang = MaGioHang FROM GioHang WHERE MaKH = @MaKH;

    SELECT 
        ISNULL(@MaGioHang, 0) AS MaGioHang,
        sp.MaSP,
        sp.TenSP,
        ISNULL(km_active.GiaKhuyenMai, sp.Gia) AS DonGia,
        sp.Gia AS GiaGoc,
        CASE WHEN km_active.GiaKhuyenMai IS NOT NULL THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END AS DangKhuyenMai,
        km_active.PhanTramGiam,
        km_active.TenKM AS TenChuongTrinhKM,
        sp.DonViTinh,
        ctgh.SoLuong,
        (ctgh.SoLuong * ISNULL(km_active.GiaKhuyenMai, sp.Gia)) AS ThanhTien,
        CASE WHEN km_active.GiaKhuyenMai IS NOT NULL 
             THEN (ctgh.SoLuong * (sp.Gia - km_active.GiaKhuyenMai)) 
             ELSE 0 END AS TienTietKiem,
        sp.SoLuongTon,
        sp.HanSuDung,
        CASE WHEN sp.SoLuongTon < ctgh.SoLuong THEN 0 ELSE 1 END AS ConDuHang
    FROM ChiTietGioHang ctgh
    INNER JOIN SanPham sp ON ctgh.MaSP = sp.MaSP
    OUTER APPLY (
        SELECT TOP 1 
            ctkm.MaKM,
            ctkm.GiaKhuyenMai,
            ctkm.PhanTramGiam,
            km.TenKM
        FROM ChiTietKhuyenMai ctkm
        INNER JOIN ChuongTrinhKhuyenMai km ON ctkm.MaKM = km.MaKM
        WHERE ctkm.MaSP = sp.MaSP
          AND km.TrangThai = 1
          AND GETDATE() BETWEEN km.NgayBatDau AND km.NgayKetThuc
          AND (ctkm.SoLuongKhuyenMai IS NULL OR ctkm.SoLuongDaBan < ctkm.SoLuongKhuyenMai)
        ORDER BY ctkm.GiaKhuyenMai ASC
    ) km_active
    WHERE ctgh.MaGioHang = @MaGioHang AND sp.TrangThaiXoa = 0;
END;
GO

-- 9. THỦ TỤC THANH TOÁN ĐƠN HÀNG VÀ TRỪ KHO AN TOÀN (REQ-TX-01 - CỐT LÕI CT221H)
CREATE OR ALTER PROCEDURE sp_ThanhToanDonHang
    @MaKH INT,
    @MaGioHang INT,
    @DiaChiGiaoHang NVARCHAR(255) = NULL,
    @GhiChu NVARCHAR(255) = NULL,
    @MaDH_Output INT OUTPUT,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    -- Bảng tạm lưu trữ chi tiết các mặt hàng mua
    DECLARE @Items TABLE (
        MaSP INT,
        TenSP NVARCHAR(200),
        SoLuongMua INT,
        GiaBan DECIMAL(18,2),
        GiaGoc DECIMAL(18,2),
        TienGiam DECIMAL(18,2),
        MaKM INT,
        ThanhTien DECIMAL(18,2)
    );

    DECLARE @TongTien DECIMAL(18,2) = 0;
    DECLARE @SoDuHienTai DECIMAL(18,2) = 0;
    DECLARE @SoDuSauGiaoDich DECIMAL(18,2) = 0;
    DECLARE @DiaChiMacDinh NVARCHAR(255) = NULL;

    BEGIN TRY
        -- 1. BẮT ĐẦU TRANSACTION ĐẢM BẢO CHUẨN ACID
        BEGIN TRANSACTION;

        -- 2. Kiểm tra tính hợp lệ của giỏ hàng
        IF NOT EXISTS (SELECT 1 FROM ChiTietGioHang WHERE MaGioHang = @MaGioHang)
        BEGIN
            SET @StatusCode = -1;
            SET @Message = N'Giỏ hàng rỗng, không thể tiến hành thanh toán.';
            SET @MaDH_Output = 0;
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- 3. Khóa dòng kiểm tra số dư ví và lấy thông tin khách hàng (UPDLOCK, ROWLOCK ngăn Lost Update)
        SELECT 
            @SoDuHienTai = SoDuVi,
            @DiaChiMacDinh = DiaChi
        FROM KhachHang WITH (UPDLOCK, ROWLOCK)
        WHERE MaKH = @MaKH;

        IF (@SoDuHienTai IS NULL)
        BEGIN
            SET @StatusCode = -4;
            SET @Message = N'Tài khoản khách hàng không tồn tại.';
            SET @MaDH_Output = 0;
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- Nếu không nhập địa chỉ riêng thì lấy địa chỉ mặc định của khách hàng
        IF (@DiaChiGiaoHang IS NULL OR LTRIM(RTRIM(@DiaChiGiaoHang)) = '')
        BEGIN
            SET @DiaChiGiaoHang = ISNULL(@DiaChiMacDinh, N'Giao tại địa chỉ đã đăng ký');
        END

        -- 4. Khóa từng dòng sản phẩm tương ứng trong SanPham và áp dụng khuyến mãi ưu đãi nhất (UPDLOCK, ROWLOCK)
        INSERT INTO @Items (MaSP, TenSP, SoLuongMua, GiaBan, GiaGoc, TienGiam, MaKM, ThanhTien)
        SELECT 
            sp.MaSP,
            sp.TenSP,
            ctgh.SoLuong,
            ISNULL(km_active.GiaKhuyenMai, sp.Gia) AS GiaBan,
            sp.Gia AS GiaGoc,
            CASE WHEN km_active.GiaKhuyenMai IS NOT NULL 
                 THEN (ctgh.SoLuong * (sp.Gia - km_active.GiaKhuyenMai)) 
                 ELSE 0 END AS TienGiam,
            km_active.MaKM,
            (ctgh.SoLuong * ISNULL(km_active.GiaKhuyenMai, sp.Gia)) AS ThanhTien
        FROM ChiTietGioHang ctgh
        INNER JOIN SanPham sp WITH (UPDLOCK, ROWLOCK) ON ctgh.MaSP = sp.MaSP
        OUTER APPLY (
            SELECT TOP 1 
                ctkm.MaKM,
                ctkm.GiaKhuyenMai,
                km.TenKM
            FROM ChiTietKhuyenMai ctkm WITH (UPDLOCK, ROWLOCK)
            INNER JOIN ChuongTrinhKhuyenMai km ON ctkm.MaKM = km.MaKM
            WHERE ctkm.MaSP = sp.MaSP
              AND km.TrangThai = 1
              AND GETDATE() BETWEEN km.NgayBatDau AND km.NgayKetThuc
              AND (ctkm.SoLuongKhuyenMai IS NULL OR ctkm.SoLuongDaBan < ctkm.SoLuongKhuyenMai)
            ORDER BY ctkm.GiaKhuyenMai ASC
        ) km_active
        WHERE ctgh.MaGioHang = @MaGioHang;

        -- 5. Kiểm tra tồn kho và hạn sử dụng từng sản phẩm tươi sống
        IF EXISTS (
            SELECT 1 
            FROM @Items item
            INNER JOIN SanPham sp ON item.MaSP = sp.MaSP
            WHERE sp.SoLuongTon < item.SoLuongMua OR sp.HanSuDung <= GETDATE() OR sp.TrangThaiXoa = 1
        )
        BEGIN
            SET @StatusCode = -3;
            SET @Message = N'Một hoặc nhiều sản phẩm trong giỏ đã hết hàng, không đủ tồn kho hoặc đã quá hạn sử dụng!';
            SET @MaDH_Output = 0;
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- 6. Tính tổng giá trị đơn hàng
        SELECT @TongTien = SUM(ThanhTien) FROM @Items;

        -- 7. Kiểm tra số dư ví điện tử của khách hàng
        IF (@SoDuHienTai < @TongTien)
        BEGIN
            SET @StatusCode = -2;
            SET @Message = N'Số dư ví không đủ thanh toán. Cần: ' + FORMAT(@TongTien, '#,##0') + 
                           N' VND, Số dư hiện có: ' + FORMAT(@SoDuHienTai, '#,##0') + N' VND.';
            SET @MaDH_Output = 0;
            ROLLBACK TRANSACTION;
            RETURN;
        END

        -- 8. THỰC HIỆN TRỪ KHO SẢN PHẨM TƯƠI SỐNG
        UPDATE sp
        SET sp.SoLuongTon = sp.SoLuongTon - item.SoLuongMua,
            sp.NgayCapNhat = GETDATE()
        FROM SanPham sp
        INNER JOIN @Items item ON sp.MaSP = item.MaSP;

        -- 9. THỰC HIỆN TRỪ TIỀN VÍ KHÁCH HÀNG
        SET @SoDuSauGiaoDich = @SoDuHienTai - @TongTien;
        UPDATE KhachHang
        SET SoDuVi = @SoDuSauGiaoDich
        WHERE MaKH = @MaKH;

        -- 10. TẠO HÓA ĐƠN ĐƠN HÀNG (Trạng thái: PAID)
        INSERT INTO DonHang (MaKH, NgayTao, TongTien, DiaChiGiaoHang, GhiChu, TrangThai)
        VALUES (@MaKH, GETDATE(), @TongTien, @DiaChiGiaoHang, @GhiChu, 'PAID');
        SET @MaDH_Output = SCOPE_IDENTITY();

        -- 11. TẠO CHI TIẾT ĐƠN HÀNG (LƯU CẢ GIÁ GỐC, TIỀN GIẢM VÀ MÃ KHUYẾN MÃI)
        INSERT INTO ChiTietDonHang (MaDH, MaSP, SoLuong, DonGia, GiaGoc, TienGiam, MaKM)
        SELECT @MaDH_Output, MaSP, SoLuongMua, GiaBan, GiaGoc, TienGiam, MaKM FROM @Items;

        -- 11.1 CẬP NHẬT SỐ LƯỢNG ĐÃ BÁN KHUYẾN MÃI
        UPDATE ctkm
        SET ctkm.SoLuongDaBan = ctkm.SoLuongDaBan + item.SoLuongMua
        FROM ChiTietKhuyenMai ctkm
        INNER JOIN @Items item ON ctkm.MaSP = item.MaSP AND ctkm.MaKM = item.MaKM
        WHERE item.MaKM IS NOT NULL;

        -- 12. GHI SỔ CÁI KIỂM TOÁN TÀI CHÍNH ACID (LichSuGiaoDichVi)
        INSERT INTO LichSuGiaoDichVi (MaKH, MaDH, LoaiGiaoDich, SoDuTruoc, SoTienThayDoi, SoDuSau, NgayGiaoDich, NoiDung)
        VALUES (
            @MaKH, 
            @MaDH_Output, 
            'THANH_TOAN', 
            @SoDuHienTai, 
            -(@TongTien), 
            @SoDuSauGiaoDich, 
            GETDATE(), 
            N'Thanh toán đơn hàng thực phẩm #' + CAST(@MaDH_Output AS NVARCHAR(20))
        );

        -- 13. XÓA CÁC MỤC TRONG GIỎ HÀNG SAU KHI MUA THÀNH CÔNG
        DELETE FROM ChiTietGioHang WHERE MaGioHang = @MaGioHang;

        -- 14. COMMIT TRANSACTION THÀNH CÔNG 100%
        COMMIT TRANSACTION;

        SET @StatusCode = 1;
        SET @Message = N'Thanh toán đơn hàng thành công qua ví điện tử. Mã đơn hàng: #' + CAST(@MaDH_Output AS NVARCHAR(20));
    END TRY
    BEGIN CATCH
        IF (XACT_STATE() <> 0)
        BEGIN
            ROLLBACK TRANSACTION;
        END
        SET @MaDH_Output = 0;
        SET @StatusCode = -99;
        SET @Message = N'Lỗi giao dịch hệ thống: ' + ERROR_MESSAGE();
    END CATCH
END;
GO

-- =========================================================================
-- PHÂN HỆ MODULE 3: AI HỌC MÁY DỰ BÁO NHU CẦU & ĐỀ XUẤT NHẬP HÀNG TƯƠI SỐNG
-- =========================================================================

-- 10. THỦ TỤC TRÍCH XUẤT LỊCH SỬ BÁN HÀNG PHỤC VỤ AI HUẤN LUYỆN / DỰ BÁO
CREATE OR ALTER PROCEDURE sp_LayLichSuBanHangHuanLuyen
    @MaSP INT = NULL,
    @SoNgayLichSu INT = 90
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @TuNgay DATETIME2 = DATEADD(DAY, -@SoNgayLichSu, GETDATE());

    SELECT 
        sp.MaSP,
        sp.TenSP,
        sp.Gia AS GiaNiemYet,
        sp.SoLuongTon,
        sp.HanSuDung,
        CAST(dh.NgayTao AS DATE) AS NgayBan,
        DATEPART(WEEKDAY, dh.NgayTao) AS DayOfWeek,
        CASE WHEN DATEPART(WEEKDAY, dh.NgayTao) IN (1, 7) THEN 1 ELSE 0 END AS IsWeekend,
        DATEPART(MONTH, dh.NgayTao) AS Month,
        DATEPART(DAY, dh.NgayTao) AS DayOfMonth,
        ISNULL(SUM(ctdh.SoLuong), 0) AS SoLuongBanNgay,
        ISNULL(AVG(ctdh.DonGia), sp.Gia) AS GiaBanTrungBinh
    FROM SanPham sp
    LEFT JOIN ChiTietDonHang ctdh ON sp.MaSP = ctdh.MaSP
    LEFT JOIN DonHang dh ON ctdh.MaDH = dh.MaDH AND dh.TrangThai = 'PAID' AND dh.NgayTao >= @TuNgay
    WHERE (@MaSP IS NULL OR sp.MaSP = @MaSP) AND sp.TrangThaiXoa = 0
    GROUP BY 
        sp.MaSP, 
        sp.TenSP, 
        sp.Gia, 
        sp.SoLuongTon, 
        sp.HanSuDung, 
        CAST(dh.NgayTao AS DATE), 
        DATEPART(WEEKDAY, dh.NgayTao), 
        DATEPART(MONTH, dh.NgayTao), 
        DATEPART(DAY, dh.NgayTao)
    ORDER BY sp.MaSP, NgayBan DESC;
END;
GO

-- 11. THỦ TỤC LƯU KẾT QUẢ DỰ BÁO AI & LIÊN KẾT CẢNH BÁO NGUY CƠ HƯ HỎNG CAO
CREATE OR ALTER PROCEDURE sp_LuuKetQuaDuBaoNhapHang
    @MaSP INT,
    @NgayDuBao DATE,
    @SoLuongDuBao DECIMAL(10,2),
    @SoLuongTon INT,
    @SoLuongNhap INT,
    @DoTinCay DECIMAL(5,2),
    @ThuatToan VARCHAR(50),
    @RuiRo VARCHAR(20),
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT,
    @MaDuBao_Output BIGINT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM SanPham WHERE MaSP = @MaSP)
        BEGIN
            SET @StatusCode = -1;
            SET @Message = N'Mã sản phẩm không tồn tại trong hệ thống.';
            SET @MaDuBao_Output = 0;
            RETURN;
        END

        -- Ghi bản ghi dự báo vào DuBaoNhapHang
        INSERT INTO DuBaoNhapHang (
            MaSP, NgayDuBao, SoLuongDuBaoTieuThu, SoLuongTonHienTai, 
            SoLuongDeXuatNhap, DoTinCay, ThuatToan, RuiRoHuHong, NgayTao
        )
        VALUES (
            @MaSP, @NgayDuBao, @SoLuongDuBao, @SoLuongTon, 
            @SoLuongNhap, @DoTinCay, ISNULL(@ThuatToan, 'PyTorch-MLP-ONNX'), @RuiRo, GETDATE()
        );

        SET @MaDuBao_Output = SCOPE_IDENTITY();

        -- Tự động liên kết ghi nhật ký cảnh báo nếu mức rủi ro hư hỏng CAO
        IF (@RuiRo = 'CAO')
        BEGIN
            DECLARE @TenSP NVARCHAR(200);
            SELECT @TenSP = TenSP FROM SanPham WHERE MaSP = @MaSP;

            INSERT INTO NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP, NguoiThucHien, NgayGhi)
            VALUES (
                'CANH_BAO_RUI_RO_THUC_PHAM',
                N'[AI Forecast Alert] Sản phẩm "' + ISNULL(@TenSP, N'') + N'" (Mã: ' + CAST(@MaSP AS NVARCHAR(10)) + 
                N') có nguy cơ quá date/ế hàng CAO! Tồn kho: ' + CAST(@SoLuongTon AS NVARCHAR(10)) + 
                N', Dự báo tiêu thụ ngày mai chỉ đạt: ' + CAST(@SoLuongDuBao AS NVARCHAR(20)) + N' đơn vị.',
                @MaSP,
                N'AI_DemandForecasting_System',
                GETDATE()
            );
        END

        SET @StatusCode = 1;
        SET @Message = N'Đã lưu kết quả dự báo AI thành công vào CSDL.';
    END TRY
    BEGIN CATCH
        SET @StatusCode = -99;
        SET @Message = N'Lỗi lưu kết quả dự báo: ' + ERROR_MESSAGE();
        SET @MaDuBao_Output = 0;
    END CATCH
END;
GO

-- =========================================================================
-- PHÂN HỆ MODULE 4: AI CHATBOT ẨM THỰC & MODEL CONTEXT PROTOCOL (MCP TOOLS)
-- =========================================================================

-- 12. THỦ TỤC THÊM NGUYÊN LIỆU MÓN ĂN VÀO GIỎ HÀNG (AI CHATBOT REQ-AI-03)
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

-- =========================================================================
-- PHÂN HỆ MODULE 7: QUẢN LÝ CHƯƠNG TRÌNH KHUYẾN MÃI & FLASH SALE
-- =========================================================================

-- 15. THỦ TỤC TẠO CHƯƠNG TRÌNH KHUYẾN MÃI MỚI
CREATE OR ALTER PROCEDURE sp_TaoChuongTrinhKhuyenMai
    @TenKM NVARCHAR(150),
    @MoTa NVARCHAR(500) = NULL,
    @LoaiKM VARCHAR(30) = 'FLASH_SALE',
    @NgayBatDau DATETIME2,
    @NgayKetThuc DATETIME2,
    @NewMaKM INT OUTPUT,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        IF (@TenKM IS NULL OR LTRIM(RTRIM(@TenKM)) = '')
        BEGIN
            SET @StatusCode = -1;
            SET @Message = N'Tên chương trình khuyến mãi không được để trống.';
            SET @NewMaKM = 0;
            RETURN;
        END

        IF (@NgayKetThuc < @NgayBatDau)
        BEGIN
            SET @StatusCode = -2;
            SET @Message = N'Thời gian kết thúc phải lớn hơn hoặc bằng thời gian bắt đầu.';
            SET @NewMaKM = 0;
            RETURN;
        END

        IF (@LoaiKM NOT IN ('FLASH_SALE', 'XA_KHO_CAN_DATE', 'GIAM_THEO_MUA', 'TRI_AN_KHACH_HANG'))
        BEGIN
            SET @LoaiKM = 'FLASH_SALE';
        END

        INSERT INTO ChuongTrinhKhuyenMai (TenKM, MoTa, LoaiKM, NgayBatDau, NgayKetThuc, TrangThai, NgayTao, NgayCapNhat)
        VALUES (@TenKM, @MoTa, @LoaiKM, @NgayBatDau, @NgayKetThuc, 1, GETDATE(), GETDATE());

        SET @NewMaKM = SCOPE_IDENTITY();
        SET @StatusCode = 1;
        SET @Message = N'Tạo chương trình khuyến mãi thành công. Mã KM: #' + CAST(@NewMaKM AS NVARCHAR(10));
    END TRY
    BEGIN CATCH
        SET @StatusCode = -99;
        SET @Message = ERROR_MESSAGE();
        SET @NewMaKM = 0;
    END CATCH
END;
GO

-- 16. THỦ TỤC THÊM SẢN PHẨM VÀO CHƯƠNG TRÌNH KHUYẾN MÃI
CREATE OR ALTER PROCEDURE sp_ThemSanPhamVaoKhuyenMai
    @MaKM INT,
    @MaSP INT,
    @GiaKhuyenMai DECIMAL(18,2),
    @SoLuongKhuyenMai INT = NULL,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM ChuongTrinhKhuyenMai WHERE MaKM = @MaKM)
        BEGIN
            SET @StatusCode = -1;
            SET @Message = N'Chương trình khuyến mãi không tồn tại.';
            RETURN;
        END

        DECLARE @GiaGoc DECIMAL(18,2);
        SELECT @GiaGoc = Gia FROM SanPham WHERE MaSP = @MaSP AND TrangThaiXoa = 0;

        IF (@GiaGoc IS NULL)
        BEGIN
            SET @StatusCode = -2;
            SET @Message = N'Sản phẩm không tồn tại hoặc đã bị xóa mềm.';
            RETURN;
        END

        IF (@GiaKhuyenMai >= @GiaGoc)
        BEGIN
            SET @StatusCode = -3;
            SET @Message = N'Giá khuyến mãi (' + FORMAT(@GiaKhuyenMai, '#,##0') + 
                           N' VND) phải nhỏ hơn giá gốc (' + FORMAT(@GiaGoc, '#,##0') + N' VND)!';
            RETURN;
        END

        IF (@GiaKhuyenMai <= 0)
        BEGIN
            SET @StatusCode = -4;
            SET @Message = N'Giá khuyến mãi phải lớn hơn 0 VND.';
            RETURN;
        END

        DECLARE @PhanTram DECIMAL(5,2) = ROUND(((@GiaGoc - @GiaKhuyenMai) / @GiaGoc) * 100.0, 2);

        MERGE ChiTietKhuyenMai AS target
        USING (SELECT @MaKM AS MaKM, @MaSP AS MaSP) AS source
        ON (target.MaKM = source.MaKM AND target.MaSP = source.MaSP)
        WHEN MATCHED THEN
            UPDATE SET 
                target.GiaKhuyenMai = @GiaKhuyenMai,
                target.PhanTramGiam = @PhanTram,
                target.SoLuongKhuyenMai = @SoLuongKhuyenMai
        WHEN NOT MATCHED THEN
            INSERT (MaKM, MaSP, GiaKhuyenMai, PhanTramGiam, SoLuongKhuyenMai, SoLuongDaBan)
            VALUES (@MaKM, @MaSP, @GiaKhuyenMai, @PhanTram, @SoLuongKhuyenMai, 0);

        SET @StatusCode = 1;
        SET @Message = N'Thêm sản phẩm vào chương trình khuyến mãi thành công! (Giảm ' + CAST(@PhanTram AS NVARCHAR(10)) + N'%)';
    END TRY
    BEGIN CATCH
        SET @StatusCode = -99;
        SET @Message = ERROR_MESSAGE();
    END CATCH
END;
GO

-- 17. THỦ TỤC TỰ ĐỘNG KÍCH HOẠT XẢ KHO THỰC PHẨM CẬN HẠN SỬ DỤNG (AI & TRIGGER HOOK)
CREATE OR ALTER PROCEDURE sp_TuDongKichHoatXaKhoCanDate
    @SoNgayCanDate INT = 2,
    @PhanTramGiamDeXuat DECIMAL(5,2) = 30.0,
    @SoLuongSanPhamDaThem INT OUTPUT,
    @MaKM_Output INT OUTPUT,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE @TenKM NVARCHAR(150) = N'Xả kho thực phẩm tươi sống cuối ngày ' + FORMAT(GETDATE(), 'dd/MM/yyyy');
        DECLARE @MaKM INT;

        SELECT TOP 1 @MaKM = MaKM 
        FROM ChuongTrinhKhuyenMai 
        WHERE LoaiKM = 'XA_KHO_CAN_DATE' 
          AND TrangThai = 1
          AND CAST(NgayKetThuc AS DATE) >= CAST(GETDATE() AS DATE);

        IF (@MaKM IS NULL)
        BEGIN
            INSERT INTO ChuongTrinhKhuyenMai (TenKM, MoTa, LoaiKM, NgayBatDau, NgayKetThuc, TrangThai, NgayTao, NgayCapNhat)
            VALUES (
                @TenKM,
                N'Chương trình xả kho thực phẩm tươi sống cận hạn sử dụng theo đề xuất của AI và Trigger hệ thống',
                'XA_KHO_CAN_DATE',
                GETDATE(),
                DATEADD(day, 1, CAST(CAST(GETDATE() AS DATE) AS DATETIME2)),
                1,
                GETDATE(),
                GETDATE()
            );
            SET @MaKM = SCOPE_IDENTITY();
        END

        SET @MaKM_Output = @MaKM;

        DECLARE @CanDateProducts TABLE (
            MaSP INT,
            GiaGoc DECIMAL(18,2),
            GiaKhuyenMai DECIMAL(18,2),
            PhanTramGiam DECIMAL(5,2)
        );

        INSERT INTO @CanDateProducts (MaSP, GiaGoc, GiaKhuyenMai, PhanTramGiam)
        SELECT 
            sp.MaSP,
            sp.Gia,
            ROUND(sp.Gia * (1.0 - (@PhanTramGiamDeXuat / 100.0)), -3),
            @PhanTramGiamDeXuat
        FROM SanPham sp
        WHERE sp.TrangThaiXoa = 0 
          AND sp.SoLuongTon > 0
          AND DATEDIFF(day, GETDATE(), sp.HanSuDung) <= @SoNgayCanDate;

        MERGE ChiTietKhuyenMai AS target
        USING @CanDateProducts AS source
        ON (target.MaKM = @MaKM AND target.MaSP = source.MaSP)
        WHEN MATCHED THEN
            UPDATE SET 
                target.GiaKhuyenMai = source.GiaKhuyenMai,
                target.PhanTramGiam = source.PhanTramGiam
        WHEN NOT MATCHED THEN
            INSERT (MaKM, MaSP, GiaKhuyenMai, PhanTramGiam, SoLuongKhuyenMai, SoLuongDaBan)
            VALUES (@MaKM, source.MaSP, source.GiaKhuyenMai, source.PhanTramGiam, NULL, 0);

        SELECT @SoLuongSanPhamDaThem = COUNT(1) FROM @CanDateProducts;

        IF (@SoLuongSanPhamDaThem > 0)
        BEGIN
            INSERT INTO NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP, NguoiThucHien, NgayGhi)
            VALUES (
                'CANH_BAO_CAN_DATE',
                N'[KÍCH HOẠT XẢ KHO TỰ ĐỘNG] Đã tự động đưa ' + CAST(@SoLuongSanPhamDaThem AS NVARCHAR(10)) + 
                N' sản phẩm cận hạn sử dụng vào chương trình [' + @TenKM + N'] với mức giảm giá ' + 
                CAST(@PhanTramGiamDeXuat AS NVARCHAR(10)) + N'%.',
                NULL,
                SUSER_SNAME(),
                GETDATE()
            );
        END

        COMMIT TRANSACTION;

        SET @StatusCode = 1;
        SET @Message = N'Đã kích hoạt chương trình xả kho thành công! Đưa ' + CAST(@SoLuongSanPhamDaThem AS NVARCHAR(10)) + 
                       N' sản phẩm cận date vào đợt giảm giá ' + CAST(@PhanTramGiamDeXuat AS NVARCHAR(10)) + N'%.';
    END TRY
    BEGIN CATCH
        IF (XACT_STATE() <> 0) ROLLBACK TRANSACTION;
        SET @StatusCode = -99;
        SET @Message = ERROR_MESSAGE();
        SET @SoLuongSanPhamDaThem = 0;
        SET @MaKM_Output = 0;
    END CATCH
END;
GO



