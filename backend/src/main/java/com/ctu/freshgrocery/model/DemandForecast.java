package com.ctu.freshgrocery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "DuBaoNhapHang")
public class DemandForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDuBao")
    private Long maDuBao;

    @Column(name = "MaSP", nullable = false)
    private Integer maSP;

    @Column(name = "NgayDuBao", nullable = false)
    private LocalDate ngayDuBao;

    @Column(name = "SoLuongDuBaoTieuThu", nullable = false, precision = 10, scale = 2)
    private BigDecimal soLuongDuBaoTieuThu;

    @Column(name = "SoLuongTonHienTai", nullable = false)
    private Integer soLuongTonHienTai;

    @Column(name = "SoLuongDeXuatNhap", nullable = false)
    private Integer soLuongDeXuatNhap;

    @Column(name = "DoTinCay", precision = 5, scale = 2)
    private BigDecimal doTinCay;

    @Column(name = "ThuatToan", nullable = false, length = 50)
    private String thuatToan;

    @Column(name = "RuiRoHuHong", nullable = false, length = 20)
    private String ruiRoHuHong;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime ngayTao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSP", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "category"})
    private Product product;
}
