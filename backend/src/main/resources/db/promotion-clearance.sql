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

    IF @SoNgayCanDate NOT BETWEEN 1 AND 30 OR @PhanTramGiamDeXuat <= 0 OR @PhanTramGiamDeXuat >= 100
    BEGIN
        SET @StatusCode = -1;
        SET @Message = N'Số ngày phải từ 1 đến 30; phần trăm giảm phải lớn hơn 0 và nhỏ hơn 100.';
        SET @SoLuongSanPhamDaThem = 0;
        SET @MaKM_Output = 0;
        RETURN;
    END;
    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE @TenKM NVARCHAR(150) = N'Xả kho thực phẩm tươi sống cuối ngày ' + FORMAT(GETDATE(), 'dd/MM/yyyy');
        DECLARE @MaKM INT;

        SELECT TOP 1 @MaKM = MaKM 
        FROM ChuongTrinhKhuyenMai 
        WHERE LoaiKM = 'XA_KHO_CAN_DATE' 
          AND TrangThai = 1
          AND NgayBatDau <= GETDATE() AND NgayKetThuc > GETDATE();

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
            ROUND(sp.Gia * (1.0 - (@PhanTramGiamDeXuat / 100.0)), 2),
            @PhanTramGiamDeXuat
        FROM SanPham sp
        WHERE sp.TrangThaiXoa = 0 
          AND sp.SoLuongTon > 0
          AND sp.HanSuDung > GETDATE()
          AND NOT EXISTS (SELECT 1 FROM SanPham parent WHERE parent.MaSP = sp.MaSPCha AND parent.TrangThaiXoa = 1)
          AND NOT EXISTS (SELECT 1 FROM SanPham child WHERE child.MaSPCha = sp.MaSP)
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
