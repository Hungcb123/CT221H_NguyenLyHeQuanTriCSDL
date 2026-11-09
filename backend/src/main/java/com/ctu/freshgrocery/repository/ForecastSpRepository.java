package com.ctu.freshgrocery.repository;

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
import java.sql.Types;
import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ForecastSpRepository {

    private final JdbcTemplate jdbcTemplate;

    private SimpleJdbcCall spLuuKetQuaDuBaoCall;
    private SimpleJdbcCall spLayLichSuBanHangCall;

    @PostConstruct
    public void init() {
        // 1. sp_LuuKetQuaDuBaoNhapHang
        this.spLuuKetQuaDuBaoCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_LuuKetQuaDuBaoNhapHang")
                .declareParameters(
                        new SqlParameter("MaSP", Types.INTEGER),
                        new SqlParameter("NgayDuBao", Types.DATE),
                        new SqlParameter("SoLuongDuBao", Types.DECIMAL),
                        new SqlParameter("SoLuongTon", Types.INTEGER),
                        new SqlParameter("SoLuongNhap", Types.INTEGER),
                        new SqlParameter("DoTinCay", Types.DECIMAL),
                        new SqlParameter("ThuatToan", Types.VARCHAR),
                        new SqlParameter("RuiRo", Types.VARCHAR),
                        new SqlOutParameter("StatusCode", Types.INTEGER),
                        new SqlOutParameter("Message", Types.NVARCHAR),
                        new SqlOutParameter("MaDuBao_Output", Types.BIGINT)
                );

        // 2. sp_LayLichSuBanHangHuanLuyen
        this.spLayLichSuBanHangCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_LayLichSuBanHangHuanLuyen")
                .declareParameters(
                        new SqlParameter("MaSP", Types.INTEGER),
                        new SqlParameter("SoNgayLichSu", Types.INTEGER)
                );

        log.info("Khởi tạo ForecastSpRepository nạp thành công Stored Procedures cho AI Module 3.");
    }

    public Map<String, Object> executeLuuKetQuaDuBao(
            Integer maSP,
            LocalDate ngayDuBao,
            BigDecimal soLuongDuBao,
            Integer soLuongTon,
            Integer soLuongNhap,
            BigDecimal doTinCay,
            String thuatToan,
            String ruiRo
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("MaSP", maSP)
                .addValue("NgayDuBao", ngayDuBao)
                .addValue("SoLuongDuBao", soLuongDuBao)
                .addValue("SoLuongTon", soLuongTon)
                .addValue("SoLuongNhap", soLuongNhap)
                .addValue("DoTinCay", doTinCay)
                .addValue("ThuatToan", thuatToan)
                .addValue("RuiRo", ruiRo);

        return spLuuKetQuaDuBaoCall.execute(params);
    }
}
