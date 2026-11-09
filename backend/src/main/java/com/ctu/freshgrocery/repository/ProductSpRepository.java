package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.dto.ProductCreateRequest;
import com.ctu.freshgrocery.dto.ProductResponse;
import com.ctu.freshgrocery.dto.ProductUpdateRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductSpRepository {

    private final JdbcTemplate jdbcTemplate;

    private SimpleJdbcCall spThemSanPhamCall;
    private SimpleJdbcCall spCapNhatSanPhamCall;
    private SimpleJdbcCall spPhanTrangSanPhamCall;
    private SimpleJdbcCall spChiTietSanPhamCall;

    @PostConstruct
    public void init() {
        // 1. Cấu hình Stored Procedure: sp_ThemSanPhamMoi
        this.spThemSanPhamCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_ThemSanPhamMoi")
                .declareParameters(
                        new SqlParameter("TenSP", Types.NVARCHAR),
                        new SqlParameter("MoTa", Types.NVARCHAR),
                        new SqlParameter("Gia", Types.DECIMAL),
                        new SqlParameter("DonViTinh", Types.NVARCHAR),
                        new SqlParameter("SoLuongTon", Types.INTEGER),
                        new SqlParameter("HanSuDung", Types.TIMESTAMP),
                        new SqlParameter("NhietDoBaoQuan", Types.NVARCHAR),
                        new SqlParameter("MaDanhMuc", Types.INTEGER),
                        new SqlOutParameter("NewMaSP", Types.INTEGER),
                        new SqlOutParameter("StatusCode", Types.INTEGER),
                        new SqlOutParameter("Message", Types.NVARCHAR)
                );

        // 2. Cấu hình Stored Procedure: sp_CapNhatSanPham
        this.spCapNhatSanPhamCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_CapNhatSanPham")
                .declareParameters(
                        new SqlParameter("MaSP", Types.INTEGER),
                        new SqlParameter("TenSP", Types.NVARCHAR),
                        new SqlParameter("MoTa", Types.NVARCHAR),
                        new SqlParameter("Gia", Types.DECIMAL),
                        new SqlParameter("DonViTinh", Types.NVARCHAR),
                        new SqlParameter("SoLuongTon", Types.INTEGER),
                        new SqlParameter("HanSuDung", Types.TIMESTAMP),
                        new SqlParameter("NhietDoBaoQuan", Types.NVARCHAR),
                        new SqlParameter("MaDanhMuc", Types.INTEGER),
                        new SqlOutParameter("StatusCode", Types.INTEGER),
                        new SqlOutParameter("Message", Types.NVARCHAR)
                );

        // 3. Cấu hình Stored Procedure: sp_LayDanhSachSanPhamPhanTrang
        this.spPhanTrangSanPhamCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_LayDanhSachSanPhamPhanTrang")
                .declareParameters(
                        new SqlParameter("MaDanhMuc", Types.INTEGER),
                        new SqlParameter("TuKhoa", Types.NVARCHAR),
                        new SqlParameter("GiaTu", Types.DECIMAL),
                        new SqlParameter("GiaDen", Types.DECIMAL),
                        new SqlParameter("SapXep", Types.VARCHAR),
                        new SqlParameter("TrangHienTai", Types.INTEGER),
                        new SqlParameter("SoDongTrenTrang", Types.INTEGER),
                        new SqlOutParameter("TongSoBanGhi", Types.INTEGER),
                        new SqlOutParameter("TongSoTrang", Types.INTEGER)
                )
                .returningResultSet("items", new ProductRowMapper());

        // 4. Cấu hình Stored Procedure: sp_LayChiTietSanPham
        this.spChiTietSanPhamCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_LayChiTietSanPham")
                .declareParameters(
                        new SqlParameter("MaSP", Types.INTEGER)
                )
                .returningResultSet("items", new ProductRowMapper());
    }

    /**
     * Gọi Stored Procedure thêm mới sản phẩm
     */
    public Map<String, Object> callThemSanPhamMoi(ProductCreateRequest req) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("TenSP", req.getTenSP())
                .addValue("MoTa", req.getMoTa())
                .addValue("Gia", req.getGia())
                .addValue("DonViTinh", req.getDonViTinh())
                .addValue("SoLuongTon", req.getSoLuongTon())
                .addValue("HanSuDung", Timestamp.valueOf(req.getHanSuDung()))
                .addValue("NhietDoBaoQuan", req.getNhietDoBaoQuan())
                .addValue("MaDanhMuc", req.getMaDanhMuc());

        log.debug("Calling Stored Procedure sp_ThemSanPhamMoi with params: {}", params.getValues());
        return spThemSanPhamCall.execute(params);
    }

    /**
     * Gọi Stored Procedure cập nhật thông tin sản phẩm
     */
    public Map<String, Object> callCapNhatSanPham(Integer maSP, ProductUpdateRequest req) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("MaSP", maSP)
                .addValue("TenSP", req.getTenSP())
                .addValue("MoTa", req.getMoTa())
                .addValue("Gia", req.getGia())
                .addValue("DonViTinh", req.getDonViTinh())
                .addValue("SoLuongTon", req.getSoLuongTon())
                .addValue("HanSuDung", req.getHanSuDung() != null ? Timestamp.valueOf(req.getHanSuDung()) : null)
                .addValue("NhietDoBaoQuan", req.getNhietDoBaoQuan())
                .addValue("MaDanhMuc", req.getMaDanhMuc());

        log.debug("Calling Stored Procedure sp_CapNhatSanPham with params: {}", params.getValues());
        return spCapNhatSanPhamCall.execute(params);
    }

    /**
     * Gọi Stored Procedure phân trang và lọc sản phẩm
     */
    public Map<String, Object> callLayDanhSachSanPhamPhanTrang(
            Integer maDanhMuc, String tuKhoa, BigDecimal giaTu, BigDecimal giaDen,
            String sapXep, int trangHienTai, int soDongTrenTrang) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("MaDanhMuc", maDanhMuc)
                .addValue("TuKhoa", tuKhoa)
                .addValue("GiaTu", giaTu)
                .addValue("GiaDen", giaDen)
                .addValue("SapXep", sapXep)
                .addValue("TrangHienTai", trangHienTai)
                .addValue("SoDongTrenTrang", soDongTrenTrang);

        log.debug("Calling Stored Procedure sp_LayDanhSachSanPhamPhanTrang with params: {}", params.getValues());
        return spPhanTrangSanPhamCall.execute(params);
    }

    /**
     * Gọi Stored Procedure lấy chi tiết sản phẩm
     */
    @SuppressWarnings("unchecked")
    public Optional<ProductResponse> callLayChiTietSanPham(Integer maSP) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("MaSP", maSP);
        Map<String, Object> result = spChiTietSanPhamCall.execute(params);
        List<ProductResponse> items = (List<ProductResponse>) result.getOrDefault("items", Collections.emptyList());
        if (items != null && !items.isEmpty()) {
            return Optional.of(items.get(0));
        }
        return Optional.empty();
    }

    /**
     * Thực thi câu lệnh DELETE kích hoạt Trigger INSTEAD OF DELETE (Soft Delete)
     */
    public int executeDeleteTrigger(Integer maSP) {
        log.info("Executing DELETE FROM SanPham WHERE MaSP = {} to trigger INSTEAD OF DELETE trigger", maSP);
        return jdbcTemplate.update("DELETE FROM SanPham WHERE MaSP = ?", maSP);
    }

    /**
     * RowMapper chuyển đổi ResultSet thành ProductResponse
     */
    public static class ProductRowMapper implements RowMapper<ProductResponse> {
        @Override
        public ProductResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp hsd = rs.getTimestamp("HanSuDung");
            Timestamp tao = rs.getTimestamp("NgayTao");
            Timestamp capNhat = rs.getTimestamp("NgayCapNhat");

            BigDecimal giaHienTai = hasColumn(rs, "GiaHienTai") ? rs.getBigDecimal("GiaHienTai") : (hasColumn(rs, "Gia") ? rs.getBigDecimal("Gia") : null);
            BigDecimal giaGoc = hasColumn(rs, "GiaGoc") ? rs.getBigDecimal("GiaGoc") : (hasColumn(rs, "Gia") ? rs.getBigDecimal("Gia") : null);

            return ProductResponse.builder()
                    .maSP(rs.getInt("MaSP"))
                    .tenSP(rs.getString("TenSP"))
                    .moTa(rs.getString("MoTa"))
                    .gia(giaHienTai != null ? giaHienTai : giaGoc)
                    .giaGoc(giaGoc)
                    .giaHienTai(giaHienTai)
                    .dangKhuyenMai(hasColumn(rs, "DangKhuyenMai") && rs.getBoolean("DangKhuyenMai"))
                    .phanTramGiam(hasColumn(rs, "PhanTramGiam") ? rs.getBigDecimal("PhanTramGiam") : BigDecimal.ZERO)
                    .tenChuongTrinhKM(hasColumn(rs, "TenChuongTrinhKM") ? rs.getString("TenChuongTrinhKM") : null)
                    .maKM(hasColumn(rs, "MaKM") ? (Integer) rs.getObject("MaKM") : null)
                    .donViTinh(rs.getString("DonViTinh"))
                    .soLuongTon(rs.getInt("SoLuongTon"))
                    .hanSuDung(hsd != null ? hsd.toLocalDateTime() : null)
                    .nhietDoBaoQuan(rs.getString("NhietDoBaoQuan"))
                    .maDanhMuc(rs.getInt("MaDanhMuc"))
                    .tenDanhMuc(rs.getString("TenDanhMuc"))
                    .trangThaiXoa(hasColumn(rs, "TrangThaiXoa") && rs.getBoolean("TrangThaiXoa"))
                    .ngayTao(tao != null ? tao.toLocalDateTime() : null)
                    .ngayCapNhat(capNhat != null ? capNhat.toLocalDateTime() : null)
                    .build();
        }

        private boolean hasColumn(ResultSet rs, String columnName) {
            try {
                rs.findColumn(columnName);
                return true;
            } catch (SQLException e) {
                return false;
            }
        }
    }
}
