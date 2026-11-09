package com.ctu.freshgrocery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CartItemId implements Serializable {

    @Column(name = "MaGioHang")
    private Integer maGioHang;

    @Column(name = "MaSP")
    private Integer maSP;
}
