package com.ctu.freshgrocery.model;

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
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "SanPham")
public class Product {
    @Column(name = "MaSPCha")
    private Integer maSPCha;


    @Column(name = "HinhAnh", length = 1000)
    private String hinhAnh;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaSP")
    private Integer maSP;

    @Column(name = "TenSP", nullable = false, length = 200)
    private String tenSP;

    @Column(name = "MoTa", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @Column(name = "Gia", nullable = false, precision = 18, scale = 2)
    private BigDecimal gia;

    @Column(name = "DonViTinh", nullable = false, length = 20)
    private String donViTinh;

    @Column(name = "SoLuongTon", nullable = false)
    private Integer soLuongTon;

    @Column(name = "HanSuDung", nullable = false)
    private LocalDateTime hanSuDung;

    @Column(name = "NhietDoBaoQuan", length = 50)
    private String nhietDoBaoQuan;

    @Column(name = "MaDanhMuc", nullable = false)
    private Integer maDanhMuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaDanhMuc", insertable = false, updatable = false)
    private Category category;

    @Column(name = "TrangThaiXoa", nullable = false)
    private Boolean trangThaiXoa;

    @Column(name = "NgayTao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "NgayCapNhat", nullable = false)
    private LocalDateTime ngayCapNhat;
}
