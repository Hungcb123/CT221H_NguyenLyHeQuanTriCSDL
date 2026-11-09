package com.ctu.freshgrocery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "LichSuGiaoDichVi")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaGiaoDich")
    private Long maGiaoDich;

    @Column(name = "MaKH", nullable = false)
    private Integer maKH;

    @Column(name = "MaDH")
    private Integer maDH;

    @Column(name = "LoaiGiaoDich", nullable = false, length = 20)
    private String loaiGiaoDich;

    @Column(name = "SoDuTruoc", nullable = false, precision = 18, scale = 2)
    private BigDecimal soDuTruoc;

    @Column(name = "SoTienThayDoi", nullable = false, precision = 18, scale = 2)
    private BigDecimal soTienThayDoi;

    @Column(name = "SoDuSau", nullable = false, precision = 18, scale = 2)
    private BigDecimal soDuSau;

    @Column(name = "NgayGiaoDich", nullable = false)
    private LocalDateTime ngayGiaoDich;

    @Column(name = "NoiDung", nullable = false, length = 255)
    private String noiDung;
}
