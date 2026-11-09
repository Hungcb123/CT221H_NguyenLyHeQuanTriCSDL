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

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "NhatKyCanhBao")
public class AlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaLog")
    private Long maLog;

    @Column(name = "LoaiSuKien", nullable = false, length = 50)
    private String loaiSuKien;

    @Column(name = "NoiDung", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    @Column(name = "MaSP")
    private Integer maSP;

    @Column(name = "NguoiThucHien", nullable = false, length = 100)
    private String nguoiThucHien;

    @Column(name = "NgayGhi", nullable = false)
    private LocalDateTime ngayGhi;
}
