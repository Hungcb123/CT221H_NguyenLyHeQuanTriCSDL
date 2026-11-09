package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.model.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
    List<AlertLog> findTop20ByOrderByNgayGhiDesc();
    List<AlertLog> findByMaSPOrderByNgayGhiDesc(Integer maSP);
    List<AlertLog> findByLoaiSuKienOrderByNgayGhiDesc(String loaiSuKien);
}
