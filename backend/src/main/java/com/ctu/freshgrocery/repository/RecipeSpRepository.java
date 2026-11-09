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

import java.sql.Types;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RecipeSpRepository {

    private final JdbcTemplate jdbcTemplate;
    private SimpleJdbcCall spThemNguyenLieuCall;

    @PostConstruct
    public void init() {
        this.spThemNguyenLieuCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_ThemNguyenLieuMonAnVaoGioHang")
                .declareParameters(
                        new SqlParameter("MaKH", Types.INTEGER),
                        new SqlParameter("MaCongThuc", Types.INTEGER),
                        new SqlParameter("KhauPhan", Types.INTEGER),
                        new SqlOutParameter("SoLuongMonDaThem", Types.INTEGER),
                        new SqlOutParameter("TongTienNguyenLieu", Types.DECIMAL),
                        new SqlOutParameter("StatusCode", Types.INTEGER),
                        new SqlOutParameter("Message", Types.NVARCHAR)
                );
        log.info("Khởi tạo RecipeSpRepository cho thủ tục sp_ThemNguyenLieuMonAnVaoGioHang.");
    }

    public Map<String, Object> executeThemNguyenLieuVaoGioHang(Integer maKH, Integer maCongThuc, Integer khauPhan) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("MaKH", maKH)
                .addValue("MaCongThuc", maCongThuc)
                .addValue("KhauPhan", khauPhan);
        return spThemNguyenLieuCall.execute(params);
    }
}
