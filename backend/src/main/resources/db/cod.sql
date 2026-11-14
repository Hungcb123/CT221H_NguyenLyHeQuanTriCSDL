CREATE OR ALTER PROCEDURE sp_ThanhToanDonHang
    @MaKH INT,
    @MaGioHang INT,
    @DiaChiGiaoHang NVARCHAR(255) = NULL,
    @GhiChu NVARCHAR(255) = NULL,
    @MaDH_Output INT OUTPUT,
    @StatusCode INT OUTPUT,
    @Message NVARCHAR(255) OUTPUT,
    @NguonDon VARCHAR(20) = 'SALE',
    @PhuongThucThanhToan VARCHAR(20) = 'WALLET'
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
        IF @PhuongThucThanhToan IS NULL OR @PhuongThucThanhToan NOT IN ('WALLET','COD')
            THROW 50001, N'Phương thức thanh toán không hợp lệ.', 1;
        BEGIN TRANSACTION;

        -- 2. Kiểm tra tính hợp lệ của giỏ hàng
        IF NOT EXISTS (SELECT 1 FROM GioHang WHERE MaGioHang = @MaGioHang AND MaKH = @MaKH)
            THROW 50001, N'Giỏ hàng không thuộc khách hàng.', 1;
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
              AND (ctkm.SoLuongKhuyenMai IS NULL OR ctgh.SoLuong <= ctkm.SoLuongKhuyenMai - ctkm.SoLuongDaBan)
            ORDER BY ctkm.GiaKhuyenMai ASC
        ) km_active
        WHERE ctgh.MaGioHang = @MaGioHang;

        -- 5. Kiểm tra tồn kho và hạn sử dụng từng sản phẩm tươi sống
        IF EXISTS (
            SELECT 1 
            FROM @Items item
            INNER JOIN SanPham sp ON item.MaSP = sp.MaSP
            WHERE sp.SoLuongTon < item.SoLuongMua OR sp.HanSuDung IS NULL OR sp.HanSuDung <= GETDATE() OR sp.TrangThaiXoa = 1
                OR EXISTS (SELECT 1 FROM SanPham parent WHERE parent.MaSP=sp.MaSPCha AND parent.TrangThaiXoa=1)
                OR EXISTS (SELECT 1 FROM SanPham child WHERE child.MaSPCha=sp.MaSP)
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
        IF (@PhuongThucThanhToan = 'WALLET' AND @SoDuHienTai < @TongTien)
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
        SET @SoDuSauGiaoDich = @SoDuHienTai;
        IF @PhuongThucThanhToan = 'WALLET'
        BEGIN
        SET @SoDuSauGiaoDich = @SoDuHienTai - @TongTien;
        UPDATE KhachHang
        SET SoDuVi = @SoDuSauGiaoDich
        WHERE MaKH = @MaKH;
        END;

        -- 10. COD chờ giao/thu tiền, WALLET đã thanh toán.
        INSERT INTO DonHang (MaKH, NgayTao, TongTien, DiaChiGiaoHang, GhiChu, TrangThai, NguonDon, PhuongThucThanhToan)
        VALUES (@MaKH, GETDATE(), @TongTien, @DiaChiGiaoHang, @GhiChu, CASE WHEN @PhuongThucThanhToan='COD' THEN 'PENDING' ELSE 'PAID' END, @NguonDon, @PhuongThucThanhToan);
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
        IF @PhuongThucThanhToan = 'WALLET'
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
        SET @Message = CASE WHEN @PhuongThucThanhToan='COD' THEN N'Đặt hàng thành công. Thanh toán khi nhận hàng. Mã đơn: #' ELSE N'Thanh toán thành công qua ví. Mã đơn: #' END + CAST(@MaDH_Output AS NVARCHAR(20));
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
