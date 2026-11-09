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
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDetailId implements Serializable {

    @Column(name = "MaKM")
    private Integer maKM;

    @Column(name = "MaSP")
    private Integer maSP;
}
