package com.ctu.freshgrocery.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ChiTietKhuyenMai")
public class PromotionDetail {

    @EmbeddedId
    private PromotionDetailId id;

    @Column(name = "GiaKhuyenMai", nullable = false, precision = 18, scale = 2)
    private BigDecimal giaKhuyenMai;

    @Column(name = "PhanTramGiam", precision = 5, scale = 2)
    private BigDecimal phanTramGiam;

    @Column(name = "SoLuongKhuyenMai")
    private Integer soLuongKhuyenMai;

    @Column(name = "SoLuongDaBan", nullable = false)
    private Integer soLuongDaBan;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKM", insertable = false, updatable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSP", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "category"})
    private Product product;
}
