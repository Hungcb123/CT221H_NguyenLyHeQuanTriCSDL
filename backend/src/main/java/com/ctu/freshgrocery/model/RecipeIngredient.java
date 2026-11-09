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
@Table(name = "NguyenLieuMonAn")
public class RecipeIngredient {

    @EmbeddedId
    private RecipeIngredientId id;

    @Column(name = "SoLuongDinhLuong", nullable = false, precision = 10, scale = 2)
    private BigDecimal soLuongDinhLuong;

    @Column(name = "DonViTinh", nullable = false, length = 20)
    private String donViTinh;

    @Column(name = "BatBuoc", nullable = false)
    private Boolean batBuoc;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaCongThuc", insertable = false, updatable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MaSP", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "category"})
    private Product product;
}
