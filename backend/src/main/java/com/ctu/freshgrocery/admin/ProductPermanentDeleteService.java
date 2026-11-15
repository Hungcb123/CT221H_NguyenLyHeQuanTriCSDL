package com.ctu.freshgrocery.admin;

import com.ctu.freshgrocery.exception.AppException;
import com.ctu.freshgrocery.service.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class ProductPermanentDeleteService {
    private final JdbcTemplate jdbc;
    private final SemanticSearchService semantic;

    @Transactional
    public int delete(int id, String actor) {
        try {
            var ids = jdbc.queryForList("EXEC dbo.sp_XoaVinhVienSanPham @MaSP = ?, @NguoiThucHien = ?", Integer.class, id, actor);
            ids.forEach(semantic::removeProduct);
            return ids.size();
        } catch (DataAccessException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && sql.getErrorCode() >= 50010 && sql.getErrorCode() <= 50015)
                    throw new AppException(sql.getErrorCode() == 50010 ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT, sql.getMessage());
            }
            throw exception;
        }
    }
}
