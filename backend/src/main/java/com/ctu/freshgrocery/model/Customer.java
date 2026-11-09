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
@Table(name = "KhachHang")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaKH")
    private Integer maKH;

    @Column(name = "HoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "Email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "SoDienThoai", nullable = false, unique = true, length = 15)
    private String soDienThoai;

    @Column(name = "DiaChi", length = 255)
    private String diaChi;

    @Column(name = "SoDuVi", nullable = false, precision = 18, scale = 2)
    private BigDecimal soDuVi;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime ngayTao;
}
