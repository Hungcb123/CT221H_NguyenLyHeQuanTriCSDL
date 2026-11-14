-- PHỤ LỤC THIẾT KẾ BÁO CÁO CT221H, 14/11/2026.
-- Chưa tích hợp database/init_db.sh, chưa chạy kiểm thử SQL Server.
-- Chạy trên database QA sau khi đã cài schema của dự án, không chạy trên dữ liệu thật để minh họa.
USE FreshGroceryDB;
GO
CREATE OR ALTER VIEW dbo.vw_SanPhamConHieuLuc
AS
SELECT sp.MaSP, sp.MaSPCha, sp.TenSP, sp.TenLoai,
       sp.MaDanhMuc, dm.TenDanhMuc, sp.Gia,
       sp.DonViTinh, sp.SoLuongTon, sp.HanSuDung
FROM dbo.SanPham AS sp
JOIN dbo.DanhMuc AS dm ON dm.MaDanhMuc = sp.MaDanhMuc
WHERE sp.TrangThaiXoa = 0
  AND sp.HanSuDung > GETDATE()
  AND sp.SoLuongTon > 0;
GO
CREATE OR ALTER VIEW dbo.vw_DoanhThuBanTheoNgay
AS
SELECT CAST(NgayTao AS date) AS Ngay,
       COUNT_BIG(*) AS SoDon,
       SUM(TongTien) AS DoanhThu
FROM dbo.DonHang
WHERE NguonDon = 'SALE'
  AND TrangThai IN ('PAID', 'DELIVERED')
GROUP BY CAST(NgayTao AS date);
GO
CREATE OR ALTER VIEW dbo.vw_TongHopMuaHangKhachHang
AS
SELECT kh.MaKH, kh.HoTen,
       COUNT_BIG(dh.MaDH) AS SoDonDaThanhToan,
       COALESCE(SUM(dh.TongTien), CAST(0 AS decimal(18,2))) AS TongChiTieu,
       MAX(dh.NgayTao) AS LanMuaGanNhat
FROM dbo.KhachHang AS kh
LEFT JOIN dbo.DonHang AS dh
  ON dh.MaKH = kh.MaKH
 AND dh.NguonDon = 'SALE'
 AND dh.TrangThai IN ('PAID', 'DELIVERED')
GROUP BY kh.MaKH, kh.HoTen;
GO
CREATE OR ALTER VIEW dbo.vw_DuBaoMoiNhatTheoSanPham
AS
WITH XepHang AS (
    SELECT MaDuBao, MaSP, NgayDuBao, SoLuongDuBaoTieuThu,
           SoLuongTonHienTai, SoLuongDeXuatNhap, RuiRoHuHong,
           ThuatToan, NgayTao,
           ROW_NUMBER() OVER (
               PARTITION BY MaSP
               ORDER BY NgayDuBao DESC, NgayTao DESC, MaDuBao DESC
           ) AS rn
    FROM dbo.DuBaoNhapHang
)
SELECT MaDuBao, MaSP, NgayDuBao, SoLuongDuBaoTieuThu,
       SoLuongTonHienTai, SoLuongDeXuatNhap,
       RuiRoHuHong, ThuatToan, NgayTao
FROM XepHang
WHERE rn = 1;
GO
