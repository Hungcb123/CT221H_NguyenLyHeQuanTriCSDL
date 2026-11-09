package com.ctu.freshgrocery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "CongThucMonAn")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaCongThuc")
    private Integer maCongThuc;

    @Column(name = "TenMonAn", nullable = false, unique = true, length = 150)
    private String tenMonAn;

    @Column(name = "MoTa", length = 500)
    private String moTa;

    @Column(name = "KhauPhanMacDinh", nullable = false)
    private Integer khauPhanMacDinh;

    @Column(name = "HuongDanNau", columnDefinition = "NVARCHAR(MAX)")
    private String huongDanNau;

    @Column(name = "NgayTao", nullable = false)
    private LocalDateTime ngayTao;

    @ToString.Exclude
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnoreProperties({"recipe"})
    private List<RecipeIngredient> ingredients = new ArrayList<>();
}
