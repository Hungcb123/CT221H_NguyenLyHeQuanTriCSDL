-- =========================================================================
-- ĐỒ ÁN CT221H: NGUYÊN LÝ HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU
-- FILE: 03_triggers.sql - Các Trigger bẫy sự kiện cập nhật & xóa mềm
-- =========================================================================

USE FreshGroceryDB;
GO

-- 1. TRIGGER BẪY BIẾN ĐỘNG GIÁ BẤT THƯỜNG, HẾT HÀNG & CẬN DATE (REQ-TRG-01)
CREATE OR ALTER TRIGGER trg_SanPham_CanhBaoBienDong
ON SanPham
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- Ghi một nhật ký cho mỗi sản phẩm có dữ liệu nghiệp vụ thay đổi.
    -- Trigger xóa mềm đã có sự kiện riêng cho UPDATE nội bộ của nó.
    IF COALESCE(TRIGGER_NESTLEVEL(OBJECT_ID(N'dbo.trg_SanPham_NganChanXoaVatLy')), 0) = 0
    BEGIN
        INSERT INTO dbo.NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP)
        SELECT 'CAP_NHAT_SAN_PHAM',
               CONCAT(N'Trước: ', cu.DuLieu, N' | Sau: ', moi.DuLieu),
               i.MaSP
        FROM inserted i
        JOIN deleted d ON d.MaSP = i.MaSP
        CROSS APPLY (
            SELECT (SELECT d.TenSP, d.MoTa, d.HinhAnh, d.Gia, d.DonViTinh, d.SoLuongTon,
                           d.HanSuDung, d.NhietDoBaoQuan, d.MaDanhMuc, d.TrangThaiXoa
                    FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES) AS DuLieu
        ) cu
        CROSS APPLY (
            SELECT (SELECT i.TenSP, i.MoTa, i.HinhAnh, i.Gia, i.DonViTinh, i.SoLuongTon,
                           i.HanSuDung, i.NhietDoBaoQuan, i.MaDanhMuc, i.TrangThaiXoa
                    FOR JSON PATH, WITHOUT_ARRAY_WRAPPER, INCLUDE_NULL_VALUES) AS DuLieu
        ) moi
        WHERE cu.DuLieu COLLATE Latin1_General_100_BIN2 <> moi.DuLieu COLLATE Latin1_General_100_BIN2;
    END;

    -- 1.1. Bẫy sự kiện CHÁY HÀNG (SoLuongTon giảm về <= 0)
    INSERT INTO NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP, NguoiThucHien, NgayGhi)
    SELECT 
        'CANH_BAO_HET_HANG',
        N'[CẢNH BÁO HẾT HÀNG] Sản phẩm [' + i.TenSP + N'] (Mã SP: ' + CAST(i.MaSP AS NVARCHAR(10)) + 
        N') vừa hết hàng trong kho (Tồn cũ: ' + CAST(d.SoLuongTon AS NVARCHAR(10)) + N', Tồn mới: ' + CAST(i.SoLuongTon AS NVARCHAR(10)) + N'). Cần lên đơn nhập hàng mới gấp.',
        i.MaSP,
        SUSER_SNAME(),
        GETDATE()
    FROM inserted i
    INNER JOIN deleted d ON i.MaSP = d.MaSP
    WHERE d.SoLuongTon > 0 AND i.SoLuongTon <= 0;

    -- 1.2. Bẫy sự kiện BIẾN ĐỘNG GIÁ BẤT THƯỜNG (Giảm sâu > 50% hoặc Tăng vọt > 200%)
    INSERT INTO NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP, NguoiThucHien, NgayGhi)
    SELECT 
        'CANH_BAO_GIA_BAT_THUONG',
        N'[CẢNH BÁO GIÁ BẤT THƯỜNG] Phát hiện biến động giá bất thường cho sản phẩm [' + i.TenSP + N'] (Mã SP: ' + CAST(i.MaSP AS NVARCHAR(10)) + 
        N'): Giá cũ = ' + FORMAT(d.Gia, '#,##0') + N' VND, Giá mới = ' + FORMAT(i.Gia, '#,##0') + 
        N' VND (Mức biến động: ' + CAST(ROUND(((i.Gia - d.Gia) / d.Gia) * 100, 1) AS NVARCHAR(20)) + N'%). Cần đối soát lỗi nhập liệu hoặc gian lận.',
        i.MaSP,
        SUSER_SNAME(),
        GETDATE()
    FROM inserted i
    INNER JOIN deleted d ON i.MaSP = d.MaSP
    WHERE (i.Gia < d.Gia * 0.5) OR (i.Gia > d.Gia * 2.0);

    -- 1.3. Bẫy sự kiện THỰC PHẨM TƯƠI SỐNG CẬN DATE (Hạn sử dụng còn <= 1 ngày)
    INSERT INTO NhatKyCanhBao (LoaiSuKien, NoiDung, MaSP, NguoiThucHien, NgayGhi)
    SELECT 
        'CANH_BAO_CAN_DATE',
        N'[CẢNH BÁO CẬN DATE] Mặt hàng thực phẩm tươi sống [' + i.TenSP + N'] (Mã SP: ' + CAST(i.MaSP AS NVARCHAR(10)) + 
        N') sắp hết hạn sử dụng vào ngày ' + CONVERT(NVARCHAR(30), i.HanSuDung, 120) + 
        N'. Số lượng tồn hiện tại: ' + CAST(i.SoLuongTon AS NVARCHAR(10)) + N' ' + i.DonViTinh + N'. Đề xuất giảm giá Flash Sale xả hàng ngay trong ngày.',
        i.MaSP,
        SUSER_SNAME(),
        GETDATE()
    FROM inserted i
    INNER JOIN deleted d ON i.MaSP = d.MaSP
    WHERE DATEDIFF(day, GETDATE(), i.HanSuDung) <= 1 AND i.SoLuongTon > 0;
END;
GO

-- 2. TRIGGER NGĂN CHẶN XÓA VẬT LÝ & THỰC THI SOFT DELETE (REQ-TRG-02)
CREATE OR ALTER TRIGGER trg_SanPham_NganChanXoaVatLy
ON SanPham
INSTEAD OF DELETE
AS
BEGIN
    SET NOCOUNT ON;

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

-- 3. TRIGGER KIỂM SOÁT TÍNH HỢP LỆ GIÁ KHUYẾN MÃI & TỰ ĐỘNG TÍNH PHẦN TRĂM GIẢM
CREATE OR ALTER TRIGGER trg_ChiTietKhuyenMai_KiemSoatGia
ON ChiTietKhuyenMai
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- 3.1. Kiểm tra nếu có bản ghi có GiaKhuyenMai >= GiaGoc của SanPham
    IF EXISTS (
        SELECT 1
        FROM inserted i
        INNER JOIN SanPham sp ON i.MaSP = sp.MaSP
        WHERE i.GiaKhuyenMai >= sp.Gia
    )
    BEGIN
        RAISERROR(N'Lỗi nghiệp vụ: Giá khuyến mãi phải nhỏ hơn giá gốc của sản phẩm!', 16, 1);
        ROLLBACK TRANSACTION;
        RETURN;
    END

    -- 3.2. Tự động tính toán và đồng bộ PhanTramGiam = ROUND(((sp.Gia - i.GiaKhuyenMai) / sp.Gia) * 100.0, 2)
    UPDATE ctkm
    SET ctkm.PhanTramGiam = ROUND(((sp.Gia - ctkm.GiaKhuyenMai) / sp.Gia) * 100.0, 2)
    FROM ChiTietKhuyenMai ctkm
    INNER JOIN inserted i ON ctkm.MaKM = i.MaKM AND ctkm.MaSP = i.MaSP
    INNER JOIN SanPham sp ON ctkm.MaSP = sp.MaSP
    WHERE ctkm.PhanTramGiam IS NULL 
       OR ctkm.PhanTramGiam <> ROUND(((sp.Gia - ctkm.GiaKhuyenMai) / sp.Gia) * 100.0, 2);
END;
GO
