package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    @Query("SELECT p FROM Promotion p WHERE p.trangThai = true AND :now BETWEEN p.ngayBatDau AND p.ngayKetThuc")
    List<Promotion> findActivePromotions(LocalDateTime now);
}
