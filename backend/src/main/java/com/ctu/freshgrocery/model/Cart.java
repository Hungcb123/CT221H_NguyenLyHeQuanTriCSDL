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

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "GioHang")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaGioHang")
    private Integer maGioHang;

    @Column(name = "MaKH", nullable = false, unique = true)
    private Integer maKH;

    @Column(name = "NgayCapNhat", nullable = false)
    private LocalDateTime ngayCapNhat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaKH", insertable = false, updatable = false)
    private Customer customer;
}
