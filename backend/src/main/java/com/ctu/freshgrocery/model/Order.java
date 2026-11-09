package com.ctu.freshgrocery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "DonHang")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaDH")
    private Integer maDH;

    @Column(name = "MaKH", nullable = false)
    private Integer maKH;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime ngayTao;

    @Column(name = "TongTien", nullable = false, precision = 18, scale = 2)
    private BigDecimal tongTien;

    @Column(name = "DiaChiGiaoHang", length = 255)
    private String diaChiGiaoHang;

    @Column(name = "GhiChu", length = 255)
    private String ghiChu;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai;

    @Column(name = "NguonDon", nullable = false, insertable = false, updatable = false)
    private String nguonDon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKH", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Customer customer;

    @ToString.Exclude
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnoreProperties({"order"})
    private List<OrderItem> items = new ArrayList<>();
}
