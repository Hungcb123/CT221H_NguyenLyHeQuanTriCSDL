package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết sản phẩm thực phẩm tươi sống")
public class ProductResponse {
    private Integer maSPCha;
    private String tenLoai;
    private String tenSanPhamCha;

    // Computed against SQL Server time, including sellable child types.
    private String trangThaiBan;
    private String hinhAnh;
    private Integer maSP;
    private String tenSP;
    private String moTa;
    private BigDecimal gia;
    private BigDecimal giaGoc;
    private BigDecimal giaHienTai;
    private Boolean dangKhuyenMai;
    private BigDecimal phanTramGiam;
    private String tenChuongTrinhKM;
    private Integer maKM;
    private String donViTinh;
    private Integer soLuongTon;
    private LocalDateTime hanSuDung;
    private String nhietDoBaoQuan;
    private Integer maDanhMuc;
    private String tenDanhMuc;
    private Boolean trangThaiXoa;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
}
