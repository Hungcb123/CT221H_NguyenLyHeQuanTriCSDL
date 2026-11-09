-- =========================================================================
-- ĐỒ ÁN CT221H: NGUYÊN LÝ HỆ QUẢN TRỊ CƠ SỞ DỮ LIỆU
-- HỆ THỐNG BÁCH HÓA THỰC PHẨM TƯƠI SỐNG (MÔ HÌNH BÁCH HÓA XANH)
-- FILE: 01_init_schema.sql - Khởi tạo CSDL và các bảng Module 1 & Module 2
-- =========================================================================

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'FreshGroceryDB')
BEGIN
    CREATE DATABASE FreshGroceryDB COLLATE Vietnamese_CI_AS;
END
GO

USE FreshGroceryDB;
GO

-- 1. BẢNG DANH MỤC THỰC PHẨM
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'DanhMuc')
BEGIN
    CREATE TABLE DanhMuc (
        MaDanhMuc INT IDENTITY(1,1) PRIMARY KEY,
        TenDanhMuc NVARCHAR(100) NOT NULL UNIQUE,
        MoTa NVARCHAR(255) NULL
    );
END
GO

-- 2. BẢNG SẢN PHẨM THỰC PHẨM TƯƠI SỐNG
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'SanPham')
BEGIN
    CREATE TABLE SanPham (
        MaSP INT IDENTITY(1,1) PRIMARY KEY,
        TenSP NVARCHAR(200) NOT NULL,
        MoTa NVARCHAR(MAX) NULL,
        HinhAnh NVARCHAR(1000) NULL,
        Gia DECIMAL(18,2) NOT NULL CHECK (Gia > 0),
        DonViTinh NVARCHAR(20) NOT NULL,
        SoLuongTon INT NOT NULL DEFAULT 0 CHECK (SoLuongTon >= 0),
        HanSuDung DATETIME2 NOT NULL,
        NhietDoBaoQuan NVARCHAR(50) NULL,
        MaDanhMuc INT NOT NULL,
        TrangThaiXoa BIT NOT NULL DEFAULT 0,
        NgayTao DATETIME2 NOT NULL DEFAULT GETDATE(),
        NgayCapNhat DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_SanPham_DanhMuc FOREIGN KEY (MaDanhMuc) REFERENCES DanhMuc(MaDanhMuc)
    );
END
GO

-- 3. BẢNG KHÁCH HÀNG & VÍ ĐIỆN TỬ
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'KhachHang')
BEGIN
    CREATE TABLE KhachHang (
        MaKH INT IDENTITY(1,1) PRIMARY KEY,
        HoTen NVARCHAR(100) NOT NULL,
        Email VARCHAR(100) NOT NULL UNIQUE,
        SoDienThoai VARCHAR(15) NOT NULL UNIQUE,
        DiaChi NVARCHAR(255) NULL,
        SoDuVi DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (SoDuVi >= 0),
        NgayTao DATETIME2 NOT NULL DEFAULT GETDATE()
    );
END
GO

-- 4. BẢNG GIỎ HÀNG
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'GioHang')
BEGIN
    CREATE TABLE GioHang (
        MaGioHang INT IDENTITY(1,1) PRIMARY KEY,
        MaKH INT NOT NULL UNIQUE,
        NgayCapNhat DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_GioHang_KhachHang FOREIGN KEY (MaKH) REFERENCES KhachHang(MaKH)
    );
END
GO

-- 5. BẢNG CHI TIẾT GIỎ HÀNG
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'ChiTietGioHang')
BEGIN
    CREATE TABLE ChiTietGioHang (
        MaGioHang INT NOT NULL,
        MaSP INT NOT NULL,
        SoLuong INT NOT NULL CHECK (SoLuong > 0),
        CONSTRAINT PK_ChiTietGioHang PRIMARY KEY (MaGioHang, MaSP),
        CONSTRAINT FK_ChiTietGioHang_GioHang FOREIGN KEY (MaGioHang) REFERENCES GioHang(MaGioHang) ON DELETE CASCADE,
        CONSTRAINT FK_ChiTietGioHang_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP)
    );
END
GO

-- 6. BẢNG ĐƠN HÀNG
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'DonHang')
BEGIN
    CREATE TABLE DonHang (
        MaDH INT IDENTITY(1,1) PRIMARY KEY,
        MaKH INT NOT NULL,
        NgayTao DATETIME2 NOT NULL DEFAULT GETDATE(),
        TongTien DECIMAL(18,2) NOT NULL CHECK (TongTien >= 0),
        DiaChiGiaoHang NVARCHAR(255) NULL,
        GhiChu NVARCHAR(255) NULL,
        TrangThai VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (TrangThai IN ('PENDING', 'PAID', 'DELIVERED', 'CANCELLED')),
        CONSTRAINT FK_DonHang_KhachHang FOREIGN KEY (MaKH) REFERENCES KhachHang(MaKH)
    );
END
GO

-- 7. BẢNG CHI TIẾT ĐƠN HÀNG
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'ChiTietDonHang')
BEGIN
    CREATE TABLE ChiTietDonHang (
        MaDH INT NOT NULL,
        MaSP INT NOT NULL,
        SoLuong INT NOT NULL CHECK (SoLuong > 0),
        DonGia DECIMAL(18,2) NOT NULL CHECK (DonGia > 0),
        GiaGoc DECIMAL(18,2) NOT NULL DEFAULT 0,
        TienGiam DECIMAL(18,2) NOT NULL DEFAULT 0,
        MaKM INT NULL,
        CONSTRAINT PK_ChiTietDonHang PRIMARY KEY (MaDH, MaSP),
        CONSTRAINT FK_ChiTietDonHang_DonHang FOREIGN KEY (MaDH) REFERENCES DonHang(MaDH) ON DELETE CASCADE,
        CONSTRAINT FK_ChiTietDonHang_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP)
    );
END
ELSE
BEGIN
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'ChiTietDonHang') AND name = 'GiaGoc')
        ALTER TABLE ChiTietDonHang ADD GiaGoc DECIMAL(18,2) NOT NULL DEFAULT 0;
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'ChiTietDonHang') AND name = 'TienGiam')
        ALTER TABLE ChiTietDonHang ADD TienGiam DECIMAL(18,2) NOT NULL DEFAULT 0;
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'ChiTietDonHang') AND name = 'MaKM')
        ALTER TABLE ChiTietDonHang ADD MaKM INT NULL;
END
GO

-- 8. BẢNG SỔ CÁI KIỂM TOÁN GIAO DỊCH VÍ ĐIỆN TỬ (AUDIT TRAIL ACID)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'LichSuGiaoDichVi')
BEGIN
    CREATE TABLE LichSuGiaoDichVi (
        MaGiaoDich BIGINT IDENTITY(1,1) PRIMARY KEY,
        MaKH INT NOT NULL,
        MaDH INT NULL,
        LoaiGiaoDich VARCHAR(20) NOT NULL CHECK (LoaiGiaoDich IN ('NAP_TIEN', 'THANH_TOAN', 'HOAN_TIEN')),
        SoDuTruoc DECIMAL(18,2) NOT NULL,
        SoTienThayDoi DECIMAL(18,2) NOT NULL,
        SoDuSau DECIMAL(18,2) NOT NULL,
        NgayGiaoDich DATETIME2 NOT NULL DEFAULT GETDATE(),
        NoiDung NVARCHAR(255) NOT NULL,
        CONSTRAINT FK_LichSuGiaoDichVi_KhachHang FOREIGN KEY (MaKH) REFERENCES KhachHang(MaKH),
        CONSTRAINT FK_LichSuGiaoDichVi_DonHang FOREIGN KEY (MaDH) REFERENCES DonHang(MaDH)
    );
END
GO

-- 9. BẢNG NHẬT KÝ CẢNH BÁO (AUDIT LOG CHO QUẢN TRỊ VIÊN)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'NhatKyCanhBao')
BEGIN
    CREATE TABLE NhatKyCanhBao (
        MaLog BIGINT IDENTITY(1,1) PRIMARY KEY,
        LoaiSuKien VARCHAR(50) NOT NULL, -- CANH_BAO_HET_HANG, CANH_BAO_GIA_BAT_THUONG, CANH_BAO_CAN_DATE, XOA_MEM_SAN_PHAM
        NoiDung NVARCHAR(MAX) NOT NULL,
        MaSP INT NULL,
        NguoiThucHien NVARCHAR(100) NOT NULL DEFAULT SUSER_SNAME(),
        NgayGhi DATETIME2 NOT NULL DEFAULT GETDATE()
    );
END
GO

-- 10. BẢNG THÔNG BÁO KHÁCH HÀNG (TRANSACTIONAL OUTBOX)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'ThongBaoKhachHang')
BEGIN
    CREATE TABLE ThongBaoKhachHang (
        MaThongBao BIGINT IDENTITY(1,1) PRIMARY KEY,
        MaKH INT NULL,
        TieuDe NVARCHAR(200) NOT NULL,
        NoiDung NVARCHAR(MAX) NOT NULL,
        TrangThaiGui VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED
        NgayTao DATETIME2 NOT NULL DEFAULT GETDATE()
    );
END
GO

-- 11. BẢNG DỰ BÁO NHU CẦU TIÊU THỤ & ĐỀ XUẤT NHẬP HÀNG (MACHINE LEARNING)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'DuBaoNhapHang')
BEGIN
    CREATE TABLE DuBaoNhapHang (
        MaDuBao BIGINT IDENTITY(1,1) PRIMARY KEY,
        MaSP INT NOT NULL,
        NgayDuBao DATE NOT NULL,
        SoLuongDuBaoTieuThu DECIMAL(10,2) NOT NULL CHECK (SoLuongDuBaoTieuThu >= 0),
        SoLuongTonHienTai INT NOT NULL,
        SoLuongDeXuatNhap INT NOT NULL CHECK (SoLuongDeXuatNhap >= 0),
        DoTinCay DECIMAL(5,2) NULL, -- NULL when no calibrated confidence is available
        ThuatToan VARCHAR(50) NOT NULL DEFAULT 'PyTorch-MLP-ONNX',
        RuiRoHuHong VARCHAR(20) NOT NULL CHECK (RuiRoHuHong IN ('THAP', 'TRUNG_BINH', 'CAO')),
        NgayTao DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_DuBaoNhapHang_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP)
    );
END
GO

-- 12. BẢNG CÔNG THỨC MÓN ĂN (AI CHATBOT ẨM THỰC)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'CongThucMonAn')
BEGIN
    CREATE TABLE CongThucMonAn (
        MaCongThuc INT IDENTITY(1,1) PRIMARY KEY,
        TenMonAn NVARCHAR(150) NOT NULL UNIQUE,
        MoTa NVARCHAR(500) NULL,
        KhauPhanMacDinh INT NOT NULL DEFAULT 4 CHECK (KhauPhanMacDinh > 0),
        HuongDanNau NVARCHAR(MAX) NULL,
        NgayTao DATETIME2 NOT NULL DEFAULT GETDATE()
    );
END
GO

-- 13. BẢNG NGUYÊN LIỆU MÓN ĂN (LIÊN KẾT SẢN PHẨM TƯƠI SỐNG)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'NguyenLieuMonAn')
BEGIN
    CREATE TABLE NguyenLieuMonAn (
        MaCongThuc INT NOT NULL,
        MaSP INT NOT NULL,
        SoLuongDinhLuong DECIMAL(10,2) NOT NULL CHECK (SoLuongDinhLuong > 0),
        DonViTinh NVARCHAR(20) NOT NULL,
        BatBuoc BIT NOT NULL DEFAULT 1,
        CONSTRAINT PK_NguyenLieuMonAn PRIMARY KEY (MaCongThuc, MaSP),
        CONSTRAINT FK_NguyenLieu_CongThuc FOREIGN KEY (MaCongThuc) REFERENCES CongThucMonAn(MaCongThuc) ON DELETE CASCADE,
        CONSTRAINT FK_NguyenLieu_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP)
    );
END
GO

-- 14. BẢNG CHƯƠNG TRÌNH KHUYẾN MÃI (PROMOTIONS & FLASH SALES)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'ChuongTrinhKhuyenMai')
BEGIN
    CREATE TABLE ChuongTrinhKhuyenMai (
        MaKM INT IDENTITY(1,1) PRIMARY KEY,
        TenKM NVARCHAR(150) NOT NULL,
        MoTa NVARCHAR(500) NULL,
        LoaiKM VARCHAR(30) NOT NULL CHECK (LoaiKM IN ('FLASH_SALE', 'XA_KHO_CAN_DATE', 'GIAM_THEO_MUA', 'TRI_AN_KHACH_HANG')),
        NgayBatDau DATETIME2 NOT NULL,
        NgayKetThuc DATETIME2 NOT NULL,
        TrangThai BIT NOT NULL DEFAULT 1, -- 1: Đang hoạt động, 0: Tạm dừng / Hết hiệu lực
        NgayTao DATETIME2 NOT NULL DEFAULT GETDATE(),
        NgayCapNhat DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT CK_KM_ThoiGian CHECK (NgayKetThuc >= NgayBatDau)
    );
END
GO

-- 15. BẢNG CHI TIẾT SẢN PHẨM KHUYẾN MÃI
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'ChiTietKhuyenMai')
BEGIN
    CREATE TABLE ChiTietKhuyenMai (
        MaKM INT NOT NULL,
        MaSP INT NOT NULL,
        GiaKhuyenMai DECIMAL(18,2) NOT NULL CHECK (GiaKhuyenMai > 0),
        PhanTramGiam DECIMAL(5,2) NULL CHECK (PhanTramGiam >= 0 AND PhanTramGiam <= 100),
        SoLuongKhuyenMai INT NULL CHECK (SoLuongKhuyenMai IS NULL OR SoLuongKhuyenMai >= 0),
        SoLuongDaBan INT NOT NULL DEFAULT 0 CHECK (SoLuongDaBan >= 0),
        CONSTRAINT PK_ChiTietKhuyenMai PRIMARY KEY (MaKM, MaSP),
        CONSTRAINT FK_CTKM_ChuongTrinh FOREIGN KEY (MaKM) REFERENCES ChuongTrinhKhuyenMai(MaKM) ON DELETE CASCADE,
        CONSTRAINT FK_CTKM_SanPham FOREIGN KEY (MaSP) REFERENCES SanPham(MaSP)
    );
END
GO

-- Thêm khóa ngoại từ ChiTietDonHang sang ChuongTrinhKhuyenMai nếu chưa có
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_ChiTietDonHang_KhuyenMai')
BEGIN
    ALTER TABLE ChiTietDonHang 
    ADD CONSTRAINT FK_ChiTietDonHang_KhuyenMai FOREIGN KEY (MaKM) REFERENCES ChuongTrinhKhuyenMai(MaKM);
END
GO


