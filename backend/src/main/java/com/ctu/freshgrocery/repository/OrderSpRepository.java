package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.dto.CartItemResponse;
import com.ctu.freshgrocery.dto.CheckoutResponse;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderSpRepository {

    private final JdbcTemplate jdbcTemplate;

    private SimpleJdbcCall spNapTienViCall;
    private SimpleJdbcCall spThemVaoGioHangCall;
    private SimpleJdbcCall spXoaKhoiGioHangCall;
    private SimpleJdbcCall spLayGioHangKhachHangCall;
    private SimpleJdbcCall spThanhToanDonHangCall;

    @PostConstruct
    public void init() {
        // 1. sp_NapTienVi
        this.spNapTienViCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_NapTienVi")
                .declareParameters(
                        new SqlParameter("MaKH", Types.INTEGER),
                        new SqlParameter("SoTien", Types.DECIMAL),
                        new SqlParameter("NoiDung", Types.NVARCHAR),
                        new SqlOutParameter("StatusCode", Types.INTEGER),
                        new SqlOutParameter("Message", Types.NVARCHAR),
                        new SqlOutParameter("SoDuMoi", Types.DECIMAL),
                        new SqlParameter("MaYeuCauNap", Types.VARCHAR)
                );

        // 2. sp_ThemVaoGioHang
        this.spThemVaoGioHangCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_ThemVaoGioHang")
                .declareParameters(
                        new SqlParameter("MaKH", Types.INTEGER),
                        new SqlParameter("MaSP", Types.INTEGER),
                        new SqlParameter("SoLuong", Types.INTEGER),
                        new SqlOutParameter("StatusCode", Types.INTEGER),
                        new SqlOutParameter("Message", Types.NVARCHAR),
                        new SqlParameter("ThayThe", Types.BIT)
                );

        // 3. sp_XoaKhoiGioHang
        this.spXoaKhoiGioHangCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_XoaKhoiGioHang")
                .declareParameters(
                        new SqlParameter("MaKH", Types.INTEGER),
                        new SqlParameter("MaSP", Types.INTEGER),
                        new SqlOutParameter("StatusCode", Types.INTEGER),
                        new SqlOutParameter("Message", Types.NVARCHAR)
                );

        // 4. sp_LayGioHangKhachHang
        this.spLayGioHangKhachHangCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_LayGioHangKhachHang")
                .declareParameters(
                        new SqlParameter("MaKH", Types.INTEGER)
                )
                .returningResultSet("items", new CartItemRowMapper());

        // 5. sp_ThanhToanDonHang (ACID Transaction)
        this.spThanhToanDonHangCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_ThanhToanDonHang")
                .declareParameters(
                        new SqlParameter("MaKH", Types.INTEGER),
                        new SqlParameter("MaGioHang", Types.INTEGER),
                        new SqlParameter("DiaChiGiaoHang", Types.NVARCHAR),
                        new SqlParameter("GhiChu", Types.NVARCHAR),
                        new SqlOutParameter("MaDH_Output", Types.INTEGER),
                        new SqlOutParameter("StatusCode", Types.INTEGER),
                        new SqlOutParameter("Message", Types.NVARCHAR),
                        new SqlParameter("NguonDon", Types.VARCHAR)
                );

        log.info("Khởi tạo OrderSpRepository và nạp thành công 5 Stored Procedures Module 2.");
    }

    public Map<String, Object> executeNapTienVi(Integer maKH, BigDecimal soTien, String noiDung) {
        return executeNapTienVi(maKH, soTien, noiDung, null);
    }

    public Map<String, Object> executeNapTienVi(Integer maKH, BigDecimal soTien, String noiDung, java.util.UUID requestId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("MaKH", maKH)
                .addValue("SoTien", soTien)
                .addValue("NoiDung", noiDung)
                .addValue("MaYeuCauNap", requestId == null ? null : requestId.toString());
        return spNapTienViCall.execute(params);
    }

    public Map<String, Object> executeThemVaoGioHang(Integer maKH, Integer maSP, Integer soLuong, boolean thayThe) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("MaKH", maKH)
                .addValue("MaSP", maSP)
                .addValue("SoLuong", soLuong)
                .addValue("ThayThe", thayThe);
        return spThemVaoGioHangCall.execute(params);
    }

    public Map<String, Object> executeXoaKhoiGioHang(Integer maKH, Integer maSP) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("MaKH", maKH)
                .addValue("MaSP", maSP);
        return spXoaKhoiGioHangCall.execute(params);
    }

    @SuppressWarnings("unchecked")
    public List<CartItemResponse> executeLayGioHangKhachHang(Integer maKH) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("MaKH", maKH);
        Map<String, Object> result = spLayGioHangKhachHangCall.execute(params);
        List<CartItemResponse> list = (List<CartItemResponse>) result.get("items");
        return list != null ? list : Collections.emptyList();
    }

    public Map<String, Object> executeThanhToanDonHang(Integer maKH, Integer maGioHang, String diaChiGiaoHang, String ghiChu) {
        return executeThanhToanDonHang(maKH, maGioHang, diaChiGiaoHang, ghiChu, "SALE");
    }

    public Map<String, Object> executeThanhToanDonHang(Integer maKH, Integer maGioHang, String diaChiGiaoHang,
                                                      String ghiChu, String nguonDon) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("MaKH", maKH)
                .addValue("MaGioHang", maGioHang)
                .addValue("DiaChiGiaoHang", diaChiGiaoHang)
                .addValue("GhiChu", ghiChu)
                .addValue("NguonDon", nguonDon);
        return spThanhToanDonHangCall.execute(params);
    }

    private static class CartItemRowMapper implements RowMapper<CartItemResponse> {
        @Override
        public CartItemResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp hanSuDungTs = rs.getTimestamp("HanSuDung");
            return CartItemResponse.builder()
                    .maSP(rs.getInt("MaSP"))
                    .tenSP(rs.getString("TenSP"))
                    .hinhAnh(rs.getString("HinhAnh"))
                    .donGia(rs.getBigDecimal("DonGia"))
                    .giaGoc(hasColumn(rs, "GiaGoc") ? rs.getBigDecimal("GiaGoc") : rs.getBigDecimal("DonGia"))
                    .dangKhuyenMai(hasColumn(rs, "DangKhuyenMai") && rs.getBoolean("DangKhuyenMai"))
                    .phanTramGiam(hasColumn(rs, "PhanTramGiam") ? rs.getBigDecimal("PhanTramGiam") : BigDecimal.ZERO)
                    .tenChuongTrinhKM(hasColumn(rs, "TenChuongTrinhKM") ? rs.getString("TenChuongTrinhKM") : null)
                    .tienTietKiem(hasColumn(rs, "TienTietKiem") ? rs.getBigDecimal("TienTietKiem") : BigDecimal.ZERO)
                    .donViTinh(rs.getString("DonViTinh"))
                    .soLuong(rs.getInt("SoLuong"))
                    .thanhTien(rs.getBigDecimal("ThanhTien"))
                    .soLuongTon(rs.getInt("SoLuongTon"))
                    .hanSuDung(hanSuDungTs != null ? hanSuDungTs.toLocalDateTime() : null)
                    .conDuHang(rs.getInt("ConDuHang") == 1)
                    .build();
        }

        private static boolean hasColumn(ResultSet rs, String columnName) {
            try {
                rs.findColumn(columnName);
                return true;
            } catch (SQLException e) {
                return false;
            }
        }
    }
}

