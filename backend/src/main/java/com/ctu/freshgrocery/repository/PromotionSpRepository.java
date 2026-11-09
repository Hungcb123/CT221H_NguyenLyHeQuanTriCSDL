package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.dto.PromotionAddProductRequest;
import com.ctu.freshgrocery.dto.PromotionCreateRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PromotionSpRepository {

    private final JdbcTemplate jdbcTemplate;

    private SimpleJdbcCall spTaoChuongTrinhKhuyenMaiCall;
    private SimpleJdbcCall spThemSanPhamVaoKhuyenMaiCall;
    private SimpleJdbcCall spTuDongKichHoatXaKhoCanDateCall;

    @PostConstruct
    public void init() {
        // 1. Thủ tục tạo chương trình khuyến mãi: sp_TaoChuongTrinhKhuyenMai
        this.spTaoChuongTrinhKhuyenMaiCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_TaoChuongTrinhKhuyenMai")
                .declareParameters(
                        new SqlParameter("TenKM", Types.NVARCHAR),
                        new SqlParameter("MoTa", Types.NVARCHAR),
                        new SqlParameter("LoaiKM", Types.VARCHAR),
                        new SqlParameter("NgayBatDau", Types.TIMESTAMP),
                        new SqlParameter("NgayKetThuc", Types.TIMESTAMP),
                        new SqlOutParameter("NewMaKM", Types.INTEGER),
                        new SqlOutParameter("StatusCode", Types.INTEGER),
                        new SqlOutParameter("Message", Types.NVARCHAR)
                );

        // 2. Thủ tục thêm sản phẩm vào khuyến mãi: sp_ThemSanPhamVaoKhuyenMai
        this.spThemSanPhamVaoKhuyenMaiCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_ThemSanPhamVaoKhuyenMai")
                .declareParameters(
                        new SqlParameter("MaKM", Types.INTEGER),
                        new SqlParameter("MaSP", Types.INTEGER),
                        new SqlParameter("GiaKhuyenMai", Types.DECIMAL),
                        new SqlParameter("SoLuongKhuyenMai", Types.INTEGER),
                        new SqlOutParameter("StatusCode", Types.INTEGER),
                        new SqlOutParameter("Message", Types.NVARCHAR)
                );

        // 3. Thủ tục tự động kích hoạt xả kho cận date: sp_TuDongKichHoatXaKhoCanDate
        this.spTuDongKichHoatXaKhoCanDateCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_TuDongKichHoatXaKhoCanDate")
                .declareParameters(
                        new SqlParameter("SoNgayCanDate", Types.INTEGER),
                        new SqlParameter("PhanTramGiamDeXuat", Types.DECIMAL),
                        new SqlOutParameter("SoLuongSanPhamDaThem", Types.INTEGER),
                        new SqlOutParameter("MaKM_Output", Types.INTEGER),
                        new SqlOutParameter("StatusCode", Types.INTEGER),
                        new SqlOutParameter("Message", Types.NVARCHAR)
                );

        log.info("Khởi tạo PromotionSpRepository và nạp thành công 3 Stored Procedures Khuyến mãi & Flash Sale.");
    }

    public Map<String, Object> executeTaoChuongTrinhKhuyenMai(PromotionCreateRequest req) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("TenKM", req.getTenKM())
                .addValue("MoTa", req.getMoTa())
                .addValue("LoaiKM", req.getLoaiKM())
                .addValue("NgayBatDau", req.getNgayBatDau() != null ? Timestamp.valueOf(req.getNgayBatDau()) : null)
                .addValue("NgayKetThuc", req.getNgayKetThuc() != null ? Timestamp.valueOf(req.getNgayKetThuc()) : null);

        log.debug("Calling Stored Procedure sp_TaoChuongTrinhKhuyenMai with params: {}", params.getValues());
        return spTaoChuongTrinhKhuyenMaiCall.execute(params);
    }

    public Map<String, Object> executeThemSanPhamVaoKhuyenMai(Integer maKM, PromotionAddProductRequest req) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("MaKM", maKM)
                .addValue("MaSP", req.getMaSP())
                .addValue("GiaKhuyenMai", req.getGiaKhuyenMai())
                .addValue("SoLuongKhuyenMai", req.getSoLuongKhuyenMai());

        log.debug("Calling Stored Procedure sp_ThemSanPhamVaoKhuyenMai with params: {}", params.getValues());
        return spThemSanPhamVaoKhuyenMaiCall.execute(params);
    }

    public Map<String, Object> executeTuDongKichHoatXaKhoCanDate(Integer soNgayCanDate, BigDecimal phanTramGiamDeXuat) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("SoNgayCanDate", soNgayCanDate != null ? soNgayCanDate : 2)
                .addValue("PhanTramGiamDeXuat", phanTramGiamDeXuat != null ? phanTramGiamDeXuat : new BigDecimal("30.00"));

        log.info("Calling Stored Procedure sp_TuDongKichHoatXaKhoCanDate: soNgayCanDate={}, phanTramGiamDeXuat={}",
                soNgayCanDate, phanTramGiamDeXuat);
        return spTuDongKichHoatXaKhoCanDateCall.execute(params);
    }
}
