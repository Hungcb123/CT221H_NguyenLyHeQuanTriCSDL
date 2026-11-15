package com.ctu.freshgrocery.admin;

import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.service.SemanticSearchService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.*;
import org.springframework.transaction.annotation.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProductPermanentDeleteService.class)
@EnabledIfEnvironmentVariable(named = "RUN_DB_INTEGRATION_TESTS", matches = "true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProductPermanentDeleteDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductPermanentDeleteService service;
    @MockBean SemanticSearchService semantic;
    int root, child, customer, cart;
    Integer promotion, recipe;
    boolean guardCreated;

    @BeforeEach void setup() throws Exception {
        jdbc.execute(new ClassPathResource("db/product-permanent-delete.sql").getContentAsString(StandardCharsets.UTF_8));
        int category = jdbc.queryForObject("SELECT TOP 1 MaDanhMuc FROM DanhMuc", Integer.class);
        root = jdbc.queryForObject("INSERT INTO SanPham(TenSP,Gia,DonViTinh,SoLuongTon,HanSuDung,MaDanhMuc) OUTPUT INSERTED.MaSP VALUES(N'QA permanent delete',10000,N'hộp',10,DATEADD(DAY,5,GETDATE()),?)", Integer.class, category);
        child = jdbc.queryForObject("INSERT INTO SanPham(MaSPCha,TenLoai,TenSP,Gia,DonViTinh,SoLuongTon,HanSuDung,MaDanhMuc) OUTPUT INSERTED.MaSP VALUES(?,N'Loại A',N'QA permanent child',10000,N'hộp',10,DATEADD(DAY,5,GETDATE()),?)", Integer.class, root, category);
        String key = UUID.randomUUID().toString();
        customer = jdbc.queryForObject("INSERT INTO KhachHang(HoTen,Email,SoDienThoai,SoDuVi) OUTPUT INSERTED.MaKH VALUES('QA permanent',?,?,0)", Integer.class, key + "@example.invalid", key.replace("-", "").substring(0,15));
        cart = jdbc.queryForObject("INSERT INTO GioHang(MaKH) OUTPUT INSERTED.MaGioHang VALUES(?)", Integer.class, customer);
    }
    @AfterEach void cleanup() {
        if (guardCreated) jdbc.execute("DROP TABLE dbo.QAPermanentDeleteGuard");
        jdbc.update("DELETE FROM DonHang WHERE MaKH=?", customer);
        jdbc.update("DELETE FROM DuBaoNhapHang WHERE MaSP IN (?,?)", root, child);
        jdbc.update("DELETE FROM NguyenLieuMonAn WHERE MaSP IN (?,?)", root, child);
        if (recipe != null) jdbc.update("DELETE FROM CongThucMonAn WHERE MaCongThuc=?", recipe);
        jdbc.update("DELETE FROM ChiTietGioHang WHERE MaGioHang=?", cart);
        jdbc.update("DELETE FROM GioHang WHERE MaGioHang=?", cart);
        jdbc.update("DELETE FROM KhachHang WHERE MaKH=?", customer);
        if (promotion != null) jdbc.update("DELETE FROM ChuongTrinhKhuyenMai WHERE MaKM=?", promotion);
        if (count("SanPham", "MaSP", root) != 0) {
            jdbc.update("UPDATE SanPham SET TrangThaiXoa=1 WHERE MaSP=?", root);
            service.delete(root, "qa-cleanup");
        }
    }
    int count(String table, String key, int id) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + key + "=?", Integer.class, id); }
    void archive() { jdbc.update("UPDATE SanPham SET TrangThaiXoa=1 WHERE MaSP=?", root); }
    void addLinks() {
        jdbc.update("INSERT INTO ChiTietGioHang(MaGioHang,MaSP,SoLuong) VALUES(?,?,1)", cart, child);
        promotion = jdbc.queryForObject("INSERT INTO ChuongTrinhKhuyenMai(TenKM,LoaiKM,NgayBatDau,NgayKetThuc) OUTPUT INSERTED.MaKM VALUES('QA permanent','FLASH_SALE',GETDATE(),DATEADD(DAY,1,GETDATE()))", Integer.class);
        jdbc.update("INSERT INTO ChiTietKhuyenMai(MaKM,MaSP,GiaKhuyenMai) VALUES(?,?,5000)", promotion, child);
    }

    @Test void activeMissingAndChildProductsCannotBePurged() {
        assertEquals(HttpStatus.CONFLICT, assertThrows(AppException.class, () -> service.delete(root,"admin")).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(AppException.class, () -> service.delete(Integer.MAX_VALUE,"admin")).getStatus());
        jdbc.update("UPDATE SanPham SET TrangThaiXoa=1 WHERE MaSP=?",child);
        assertEquals(HttpStatus.CONFLICT, assertThrows(AppException.class, () -> service.delete(child,"admin")).getStatus());
        assertEquals(1,count("SanPham","MaSP",root));
    }
    @Test void purgeRemovesTheWholeFamilyAndEphemeralLinksButKeepsAudit() {
        addLinks(); archive();
        assertEquals(2,service.delete(root,"qa-admin"));
        assertEquals(0,count("SanPham","MaSP",root)); assertEquals(0,count("SanPham","MaSP",child));
        assertEquals(0,count("ChiTietGioHang","MaGioHang",cart));
        assertEquals(0,count("ChiTietKhuyenMai","MaKM",promotion));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM NhatKyCanhBao WHERE MaSP IN (?,?) AND LoaiSuKien='XOA_VINH_VIEN_SAN_PHAM' AND NguoiThucHien='qa-admin'",Integer.class,root,child));
        verify(semantic).removeProduct(root); verify(semantic).removeProduct(child);
        assertEquals(HttpStatus.NOT_FOUND,assertThrows(AppException.class,()->service.delete(root,"admin")).getStatus());
    }
    @Test void anyChildOrderHistoryBlocksTheWholePurge() {
        int order=jdbc.queryForObject("INSERT INTO DonHang(MaKH,TongTien,TrangThai,NguonDon) OUTPUT INSERTED.MaDH VALUES(?,10000,'CANCELLED','SALE')",Integer.class,customer);
        jdbc.update("INSERT INTO ChiTietDonHang(MaDH,MaSP,SoLuong,DonGia) VALUES(?,?,1,10000)",order,child);
        addLinks(); archive();
        assertTrue(assertThrows(AppException.class,()->service.delete(root,"admin")).getMessage().contains("đơn hàng"));
        assertEquals(1,count("ChiTietDonHang","MaDH",order));
        assertEquals(1,count("SanPham","MaSP",root)); assertEquals(1,count("SanPham","MaSP",child));
        assertEquals(1,count("ChiTietGioHang","MaGioHang",cart));
        assertEquals(1,count("ChiTietKhuyenMai","MaKM",promotion));
    }
    @Test void forecastingAndRecipeReferencesArePreserved() {
        jdbc.update("INSERT INTO DuBaoNhapHang(MaSP,NgayDuBao,SoLuongDuBaoTieuThu,SoLuongTonHienTai,SoLuongDeXuatNhap,RuiRoHuHong) VALUES(?,CAST(GETDATE() AS DATE),1,10,0,'THAP')",child);
        archive();
        assertTrue(assertThrows(AppException.class,()->service.delete(root,"admin")).getMessage().contains("dự báo"));
        jdbc.update("DELETE FROM DuBaoNhapHang WHERE MaSP=?",child);
        recipe=jdbc.queryForObject("INSERT INTO CongThucMonAn(TenMonAn) OUTPUT INSERTED.MaCongThuc VALUES(?)",Integer.class,"QA recipe "+UUID.randomUUID());
        jdbc.update("INSERT INTO NguyenLieuMonAn(MaCongThuc,MaSP,SoLuongDinhLuong,DonViTinh) VALUES(?,?,1,N'hộp')",recipe,child);
        assertTrue(assertThrows(AppException.class,()->service.delete(root,"admin")).getMessage().contains("công thức"));
        assertEquals(1,count("NguyenLieuMonAn","MaCongThuc",recipe));
    }
    @Test void physicalFailureRollsBackChildrenCartAndPromotionAndClearsConnectionContext() {
        addLinks(); archive();
        jdbc.execute("CREATE TABLE dbo.QAPermanentDeleteGuard(MaSP INT REFERENCES dbo.SanPham(MaSP))"); guardCreated=true;
        jdbc.update("INSERT INTO dbo.QAPermanentDeleteGuard VALUES(?)",root);
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (var statement=connection.prepareStatement("EXEC dbo.sp_XoaVinhVienSanPham ?, N'qa-admin'")) {
                statement.setInt(1,root); assertThrows(SQLException.class,statement::execute);
            }
            try (var statement=connection.createStatement(); var result=statement.executeQuery("SELECT SESSION_CONTEXT(N'DailyMartPurgeProductId')")) {
                assertTrue(result.next()); assertNull(result.getObject(1));
            }
            return null;
        });
        assertEquals(1,count("SanPham","MaSP",root)); assertEquals(1,count("SanPham","MaSP",child));
        assertEquals(1,count("ChiTietGioHang","MaGioHang",cart));
        assertEquals(1,count("ChiTietKhuyenMai","MaKM",promotion));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM NhatKyCanhBao WHERE MaSP=? AND LoaiSuKien='XOA_VINH_VIEN_SAN_PHAM'",Integer.class,root));
    }
    @Test void successfulPurgeRestoresContextAndNormalDeleteStillUsesSoftDelete() {
        archive();
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (var statement=connection.createStatement()) {
                statement.execute("EXEC sys.sp_set_session_context N'DailyMartPurgeProductId', 123456");
                try (var call=connection.prepareStatement("EXEC dbo.sp_XoaVinhVienSanPham ?, N'qa-admin'")) { call.setInt(1,root); call.execute(); }
                try (var result=statement.executeQuery("SELECT SESSION_CONTEXT(N'DailyMartPurgeProductId')")) { assertTrue(result.next()); assertEquals(123456,result.getInt(1)); }
                finally { statement.execute("EXEC sys.sp_set_session_context N'DailyMartPurgeProductId', NULL"); }
            }
            return null;
        });
        int other=jdbc.queryForObject("INSERT INTO SanPham(TenSP,Gia,DonViTinh,SoLuongTon,HanSuDung,MaDanhMuc) OUTPUT INSERTED.MaSP SELECT N'QA still soft',10000,N'hộp',1,DATEADD(DAY,1,GETDATE()),MaDanhMuc FROM DanhMuc WHERE MaDanhMuc=(SELECT MIN(MaDanhMuc) FROM DanhMuc)",Integer.class);
        try {
            jdbc.update("DELETE FROM SanPham WHERE MaSP=?",other);
            assertTrue(jdbc.queryForObject("SELECT TrangThaiXoa FROM SanPham WHERE MaSP=?",Boolean.class,other));
        } finally { service.delete(other,"qa-cleanup"); }
    }
}
