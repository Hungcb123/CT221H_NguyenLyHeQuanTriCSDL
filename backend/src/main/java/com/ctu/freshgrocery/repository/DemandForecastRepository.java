package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.model.DemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DemandForecastRepository extends JpaRepository<DemandForecast, Long> {
    List<DemandForecast> findByMaSPOrderByNgayTaoDesc(Integer maSP);
    List<DemandForecast> findByNgayDuBaoOrderByMaSPAsc(LocalDate ngayDuBao);
}
