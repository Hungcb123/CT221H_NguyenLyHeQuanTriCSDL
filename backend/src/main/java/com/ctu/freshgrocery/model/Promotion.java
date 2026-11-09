package com.ctu.freshgrocery.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ChuongTrinhKhuyenMai")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaKM")
    private Integer maKM;

    @Column(name = "TenKM", nullable = false, length = 150)
    private String tenKM;

    @Column(name = "MoTa", length = 500)
    private String moTa;

    @Column(name = "LoaiKM", nullable = false, length = 30)
    private String loaiKM; // FLASH_SALE, XA_KHO_CAN_DATE, GIAM_THEO_MUA, TRI_AN_KHACH_HANG

    @Column(name = "NgayBatDau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "NgayKetThuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "TrangThai", nullable = false)
    private Boolean trangThai;

    @Column(name = "NgayTao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "NgayCapNhat", nullable = false)
    private LocalDateTime ngayCapNhat;

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PromotionDetail> details = new ArrayList<>();

    public List<PromotionDetail> getChiTietKhuyenMai() {
        return details;
    }
}
