-- =========================================================================
-- ĐỒ ÁN CT221H: NGUYÊN LÝ HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU
-- FILE: 04_seed_data.sql - Dữ liệu mẫu ban đầu cho Danh Mục & Sản Phẩm
-- =========================================================================

USE FreshGroceryDB;
GO

-- 1. NẠP DỮ LIỆU DANH MỤC
IF NOT EXISTS (SELECT 1 FROM DanhMuc)
BEGIN
    SET IDENTITY_INSERT DanhMuc ON;
    INSERT INTO DanhMuc (MaDanhMuc, TenDanhMuc, MoTa) VALUES
    (1, N'Rau củ quả tươi sạch', N'Các loại rau ăn lá, củ, quả hữu cơ đạt tiêu chuẩn VietGAP canh tác tự nhiên'),
    (2, N'Thịt tươi sống mỗi ngày', N'Thịt heo, thịt bò, thịt gà tươi mổ trong ngày, kiểm dịch thú y an toàn'),
    (3, N'Thủy hải sản tươi sống', N'Cá sông, cá biển, tôm, mực tươi đánh bắt bảo quản lạnh chuyên dụng'),
    (4, N'Trái cây tươi nhiệt đới', N'Trái cây đặc sản miền Tây, miền Đông chín cây tự nhiên, vị ngọt thanh'),
    (5, N'Gia vị & Đồ khô bách hóa', N'Gia vị nêm nếm, đồ khô thiết yếu phục vụ nấu ăn gia đình');
    SET IDENTITY_INSERT DanhMuc OFF;
END
GO

-- 2. NẠP DỮ LIỆU SẢN PHẨM TƯƠI SỐNG
IF NOT EXISTS (SELECT 1 FROM SanPham)
BEGIN
    SET IDENTITY_INSERT SanPham ON;
    INSERT INTO SanPham (MaSP, TenSP, MoTa, Gia, DonViTinh, SoLuongTon, HanSuDung, NhietDoBaoQuan, MaDanhMuc, TrangThaiXoa, NgayTao, NgayCapNhat) VALUES
    (1, N'Cá lóc tươi làm sạch', N'Cá lóc đồng làm sạch mang và ruột, cắt khúc sẵn sàng chế biến món canh chua', 75000, N'con', 25, DATEADD(DAY, 2, GETDATE()), N'Bảo quản mát 0 - 4°C', 3, 0, GETDATE(), GETDATE()),
    (2, N'Thịt ba rọi heo VietGAP', N'Thịt ba chỉ heo tươi mềm, tỷ lệ nạc mỡ cân đối, thích hợp kho tàu hoặc luộc', 145000, N'kg', 40, DATEADD(DAY, 3, GETDATE()), N'Bảo quản mát 0 - 2°C', 2, 0, GETDATE(), GETDATE()),
    (3, N'Rau muống nước sạch', N'Rau muống non ngọn xanh giòn, trồng thủy canh không hóa chất', 12000, N'bó', 80, DATEADD(DAY, 2, GETDATE()), N'Nơi thoáng mát hoặc ngăn mát', 1, 0, GETDATE(), GETDATE()),
    (4, N'Cà chua tươi Đà Lạt', N'Cà chua chín mọng đỏ tự nhiên, giàu vitamin C và khoáng chất', 32000, N'kg', 50, DATEADD(DAY, 5, GETDATE()), N'Nhiệt độ phòng hoặc ngăn rau củ', 1, 0, GETDATE(), GETDATE()),
    (5, N'Trứng gà ta Ba Huân (vỉ 10)', N'Trứng gà tươi sạch tiệt trùng UV, lòng đỏ béo ngậy', 35000, N'vỉ', 65, DATEADD(DAY, 20, GETDATE()), N'Bảo quản nơi khô ráo mát mẻ', 2, 0, GETDATE(), GETDATE()),
    (6, N'Tôm sú tươi sống', N'Tôm sú sinh thái Cà Mau thịt chắc ngọt tự nhiên', 260000, N'kg', 18, DATEADD(DAY, 2, GETDATE()), N'Ướp đá lạnh hoặc ngăn đá', 3, 0, GETDATE(), GETDATE()),
    (7, N'Bắp cải trắng Đà Lạt', N'Bắp cải cuộn chặt búp, giòn ngọt, thích hợp xào hoặc nấu canh thịt bằm', 22000, N'bắp', 35, DATEADD(DAY, 7, GETDATE()), N'Bảo quản ngăn mát tủ lạnh', 1, 0, GETDATE(), GETDATE()),
    (8, N'Chuối sứ chín tự nhiên', N'Chuối sứ Bến Tre dẻo thơm, ngọt dịu tráng miệng', 28000, N'nải', 45, DATEADD(DAY, 4, GETDATE()), N'Nhiệt độ phòng thoáng khí', 4, 0, GETDATE(), GETDATE()),
    (9, N'Cá hồi phi lê Nauy tươi', N'Thịt cá hồi màu cam tươi đẹp mắt, giàu Omega-3, ăn sống Sashimi hoặc áp chảo', 480000, N'kg', 12, DATEADD(DAY, 2, GETDATE()), N'Bảo quản lạnh -1 đến 2°C', 3, 0, GETDATE(), GETDATE()),
    (10, N'Thịt bò bắp Úc tươi', N'Bắp bò giòn sần sật có vân hoa đẹp mắt, thích hợp nhúng lẩu hoặc hầm tiêu', 290000, N'kg', 20, DATEADD(DAY, 4, GETDATE()), N'Bảo quản mát 0 - 2°C', 2, 0, GETDATE(), GETDATE()),
    (11, N'Xoài cát Hòa Lộc', N'Xoài cát Tiền Giang vỏ vàng mịn, vị ngọt đậm đà hương thơm đặc trưng', 85000, N'kg', 30, DATEADD(DAY, 5, GETDATE()), N'Nhiệt độ phòng mát', 4, 0, GETDATE(), GETDATE()),
    (12, N'Đậu hũ non trứng', N'Đậu hũ non mềm mịn béo ngậy, thích hợp nấu canh rong biển hoặc chiên giòn', 15000, N'hộp', 55, DATEADD(DAY, 10, GETDATE()), N'Ngăn mát 2 - 5°C', 1, 0, GETDATE(), GETDATE());
    SET IDENTITY_INSERT SanPham OFF;
END
GO

-- 3. NẠP DỮ LIỆU KHÁCH HÀNG MẪU & VÍ ĐIỆN TỬ
IF NOT EXISTS (SELECT 1 FROM KhachHang)
BEGIN
    SET IDENTITY_INSERT KhachHang ON;
    INSERT INTO KhachHang (MaKH, HoTen, Email, SoDienThoai, DiaChi, SoDuVi, NgayTao) VALUES
    (1, N'Nguyễn Văn An', 'nguyenvanan@ctu.edu.vn', '0901234567', N'123 Đường 3/2, Phường Xuân Khánh, Quận Ninh Kiều, Cần Thơ', 1000000.00, GETDATE()),
    (2, N'Trần Thị Bình', 'tranthibinh@ctu.edu.vn', '0912345678', N'456 Đường Nguyễn Văn Cừ, Phường An Khánh, Quận Ninh Kiều, Cần Thơ', 500000.00, GETDATE()),
    (3, N'Lê Văn Cường', 'levancuong@ctu.edu.vn', '0923456789', N'789 Đường 30/4, Phường Hưng Lợi, Quận Ninh Kiều, Cần Thơ', 50000.00, GETDATE());
    SET IDENTITY_INSERT KhachHang OFF;

    -- Ghi sổ cái khởi tạo số dư ví ban đầu
    INSERT INTO LichSuGiaoDichVi (MaKH, MaDH, LoaiGiaoDich, SoDuTruoc, SoTienThayDoi, SoDuSau, NgayGiaoDich, NoiDung) VALUES
    (1, NULL, 'NAP_TIEN', 0, 1000000.00, 1000000.00, GETDATE(), N'Khởi tạo số dư ví ban đầu cho khách hàng VIP'),
    (2, NULL, 'NAP_TIEN', 0, 500000.00, 500000.00, GETDATE(), N'Khởi tạo số dư ví ban đầu cho khách hàng thân thiết'),
    (3, NULL, 'NAP_TIEN', 0, 50000.00, 50000.00, GETDATE(), N'Khởi tạo số dư ví ban đầu cho khách hàng mới');
END
GO

-- 4. NẠP DỮ LIỆU GIỎ HÀNG BAN ĐẦU
IF NOT EXISTS (SELECT 1 FROM GioHang)
BEGIN
    SET IDENTITY_INSERT GioHang ON;
    INSERT INTO GioHang (MaGioHang, MaKH, NgayCapNhat) VALUES
    (1, 1, GETDATE()),
    (2, 2, GETDATE()),
    (3, 3, GETDATE());
    SET IDENTITY_INSERT GioHang OFF;

    -- Thêm các món hàng mẫu vào giỏ hàng của Khách 1 và Khách 2
    INSERT INTO ChiTietGioHang (MaGioHang, MaSP, SoLuong) VALUES
    (1, 1, 2), -- Khách 1: 2 con cá lóc tươi (2 * 75.000 = 150.000)
    (1, 4, 1), -- Khách 1: 1 kg cà chua tươi (1 * 32.000 = 32.000) -> Tổng = 182.000 VND
    (2, 3, 3); -- Khách 2: 3 bó rau muống nước (3 * 12.000 = 36.000 VND)
END
GO

-- 5. NẠP DỮ LIỆU CÔNG THỨC MÓN ĂN & NGUYÊN LIỆU (AI CHATBOT)
IF NOT EXISTS (SELECT 1 FROM CongThucMonAn)
BEGIN
    SET IDENTITY_INSERT CongThucMonAn ON;
    INSERT INTO CongThucMonAn (MaCongThuc, TenMonAn, MoTa, KhauPhanMacDinh, HuongDanNau, NgayTao) VALUES
    (1, N'Canh chua cá lóc miền Tây', 
        N'Món canh chua truyền thống đồng bằng sông Cửu Long, vị chua thanh ngọt béo, ăn kèm rau muống và cà chua giòn mát', 
        4, 
        N'1. Rửa sạch cá lóc, cắt khúc vừa ăn. 2. Đun sôi nước với me dầm lấy nước chua. 3. Thả cá lóc vào nấu chín tới. 4. Cho cà chua bổ múi cau, rau muống cắt khúc vào sôi bùng. 5. Nêm nước mắm ngon, ớt lát và ngò gai.', 
        GETDATE()),
    (2, N'Thịt ba rọi kho tàu trứng gà', 
        N'Món kho đậm đà màu cánh gián tự nhiên, thịt ba rọi mềm tan béo ngậy kết hợp trứng gà ta bùi thơm', 
        4, 
        N'1. Thịt ba rọi cắt miếng vuông 3-4cm, ướp hành tỏi, đường, nước mắm trong 30 phút. 2. Luộc trứng gà ta, bóc vỏ. 3. Thắng nước màu caramen, cho thịt vào xào săn. 4. Đổ nước dừa tươi ngập thịt, kho lửa nhỏ đến khi thịt mềm rồi cho trứng vào kho thêm 15 phút.', 
        GETDATE()),
    (3, N'Canh đậu hũ non bắp cải thanh đạm', 
        N'Món canh thuần chay thanh lọc cơ thể, ngọt dịu tự nhiên từ bắp cải cuộn và đậu hũ non trứng mềm mượt', 
        3, 
        N'1. Bắp cải thái sợi vừa ăn, rửa sạch để ráo. 2. Cà chua cắt múi cau. 3. Đậu hũ non trứng cắt khối vuông nhỏ nhẹ tay. 4. Nấu sôi nước luộc rau củ, cho bắp cải và cà chua vào nấu chín tới. 5. Thả đậu hũ non vào sau cùng, nêm muối tiêu vừa miệng.', 
        GETDATE());
    SET IDENTITY_INSERT CongThucMonAn OFF;

    -- Định lượng nguyên liệu cho từng món
    INSERT INTO NguyenLieuMonAn (MaCongThuc, MaSP, SoLuongDinhLuong, DonViTinh, BatBuoc) VALUES
    -- Món 1: Canh chua cá lóc (Khẩu phần 4 người)
    (1, 1, 1, N'con', 1),  -- Cá lóc tươi làm sạch
    (1, 4, 1, N'kg', 1),   -- Cà chua tươi Đà Lạt
    (1, 3, 2, N'bó', 1),   -- Rau muống nước sạch
    -- Món 2: Thịt kho tàu (Khẩu phần 4 người)
    (2, 2, 1, N'kg', 1),   -- Thịt ba rọi heo VietGAP
    (2, 5, 1, N'vỉ', 1),   -- Trứng gà ta Ba Huân (vỉ 10)
    -- Món 3: Canh đậu hũ non bắp cải (Khẩu phần 3 người)
    (3, 12, 2, N'hộp', 1), -- Đậu hũ non trứng
    (3, 7, 1, N'bắp', 1),  -- Bắp cải trắng Đà Lạt
    (3, 4, 1, N'kg', 1);   -- Cà chua tươi Đà Lạt
END
GO

-- 6. NẠP DỮ LIỆU MẪU CHƯƠNG TRÌNH KHUYẾN MÃI (FLASH SALE & NÔNG SẢN)
IF NOT EXISTS (SELECT 1 FROM ChuongTrinhKhuyenMai)
BEGIN
    SET IDENTITY_INSERT ChuongTrinhKhuyenMai ON;
    INSERT INTO ChuongTrinhKhuyenMai (MaKM, TenKM, MoTa, LoaiKM, NgayBatDau, NgayKetThuc, TrangThai, NgayTao, NgayCapNhat) VALUES
    (1, N'Flash Sale Giờ Vàng Thủy Hải Sản', N'Ưu đãi giảm giá đặc biệt trong ngày cho mặt hàng tôm sú tươi sống', 'FLASH_SALE', DATEADD(day, -1, GETDATE()), DATEADD(day, 3, GETDATE()), 1, GETDATE(), GETDATE()),
    (2, N'Lễ Hội Nông Sản Sạch Đà Lạt', N'Giảm giá nông sản rau củ quả tươi từ nhà vườn Đà Lạt', 'GIAM_THEO_MUA', DATEADD(day, -2, GETDATE()), DATEADD(day, 5, GETDATE()), 1, GETDATE(), GETDATE());
    SET IDENTITY_INSERT ChuongTrinhKhuyenMai OFF;

    INSERT INTO ChiTietKhuyenMai (MaKM, MaSP, GiaKhuyenMai, PhanTramGiam, SoLuongKhuyenMai, SoLuongDaBan) VALUES
    (1, 6, 208000.00, 20.00, 50, 2),
    (2, 4, 25000.00, 21.88, 100, 5),
    (2, 10, 232000.00, 20.00, 30, 0);
END
GO

-- 7. NẠP DỮ LIỆU ĐƠN HÀNG LỊCH SỬ 7 NGÀY GẦN NHẤT CHO 12 SẢN PHẨM (PHỤC VỤ AI FORECAST LAG & ROLLING)
IF NOT EXISTS (SELECT 1 FROM ChiTietDonHang)
BEGIN
    DECLARE @DayIdx INT = 6;
    WHILE @DayIdx >= 0
    BEGIN
        DECLARE @OrderDate DATETIME2 = DATEADD(HOUR, 10, CAST(DATEADD(DAY, -@DayIdx, CAST(GETDATE() AS DATE)) AS DATETIME2));
        DECLARE @CustomerId INT = ((6 - @DayIdx) % 3) + 1;
        DECLARE @Mult FLOAT = CASE @DayIdx
            WHEN 6 THEN 0.92
            WHEN 5 THEN 0.96
            WHEN 4 THEN 1.00
            WHEN 3 THEN 1.04
            WHEN 2 THEN 1.08
            WHEN 1 THEN 1.25
            ELSE 1.12
        END;

        DECLARE @NewOrderId INT;
        INSERT INTO DonHang (MaKH, NgayTao, TongTien, DiaChiGiaoHang, GhiChu, TrangThai)
        VALUES (
            @CustomerId,
            @OrderDate,
            0,
            N'Quận Ninh Kiều, TP. Cần Thơ',
            N'Đơn hàng lịch sử 7 ngày phục vụ thống kê & AI dự báo nhu cầu',
            'DELIVERED'
        );
        SET @NewOrderId = SCOPE_IDENTITY();

        INSERT INTO ChiTietDonHang (MaDH, MaSP, SoLuong, DonGia, GiaGoc, TienGiam, MaKM)
        SELECT
            @NewOrderId,
            sp.MaSP,
            CAST(ROUND(
                CASE sp.MaSP
                    WHEN 1 THEN 45
                    WHEN 2 THEN 60
                    WHEN 3 THEN 85
                    WHEN 4 THEN 55
                    WHEN 5 THEN 70
                    WHEN 6 THEN 30
                    WHEN 7 THEN 40
                    WHEN 8 THEN 50
                    WHEN 9 THEN 18
                    WHEN 10 THEN 28
                    WHEN 11 THEN 42
                    WHEN 12 THEN 75
                    ELSE 30
                END * @Mult, 0) AS INT),
            sp.Gia,
            sp.Gia,
            0,
            NULL
        FROM SanPham sp
        WHERE sp.TrangThaiXoa = 0;

        UPDATE dh
        SET dh.TongTien = (
            SELECT ISNULL(SUM(ct.SoLuong * ct.DonGia), 0)
            FROM ChiTietDonHang ct
            WHERE ct.MaDH = @NewOrderId
        )
        FROM DonHang dh
        WHERE dh.MaDH = @NewOrderId;

        SET @DayIdx = @DayIdx - 1;
    END
END
GO



