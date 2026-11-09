package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.model.PromotionDetail;
import com.ctu.freshgrocery.model.PromotionDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionDetailRepository extends JpaRepository<PromotionDetail, PromotionDetailId> {

    @Query("SELECT d FROM PromotionDetail d " +
           "INNER JOIN d.promotion p " +
           "WHERE d.id.maSP = :maSP " +
           "  AND p.trangThai = true " +
           "  AND :now BETWEEN p.ngayBatDau AND p.ngayKetThuc " +
           "  AND (d.soLuongKhuyenMai IS NULL OR d.soLuongDaBan < d.soLuongKhuyenMai) " +
           "ORDER BY d.giaKhuyenMai ASC")
    List<PromotionDetail> findActiveDiscountsForProduct(@Param("maSP") Integer maSP, @Param("now") LocalDateTime now);

    List<PromotionDetail> findByIdMaKM(Integer maKM);

    java.util.Optional<PromotionDetail> findByIdMaKMAndIdMaSP(Integer maKM, Integer maSP);
}
