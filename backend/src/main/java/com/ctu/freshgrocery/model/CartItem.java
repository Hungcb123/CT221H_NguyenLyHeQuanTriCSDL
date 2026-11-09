package com.ctu.freshgrocery.model;

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

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ChiTietGioHang")
public class CartItem {

    @EmbeddedId
    private CartItemId id;

    @Column(name = "SoLuong", nullable = false)
    private Integer soLuong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaGioHang", insertable = false, updatable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSP", insertable = false, updatable = false)
    private Product product;
}
