package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Thông tin thêm mới sản phẩm tươi sống")
public class ProductCreateRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Schema(description = "Tên mặt hàng thực phẩm", example = "Cá lóc tươi làm sạch")
    private String tenSP;

    @Schema(description = "Mô tả chi tiết và hướng dẫn sơ chế", example = "Cá lóc đồng làm sạch ruột, cắt khúc nấu canh chua")
    private String moTa;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.01", message = "Giá bán phải lớn hơn 0 VND")
    @Schema(description = "Giá bán niêm yết (VND)", example = "75000.00")
    private BigDecimal gia;

    @NotBlank(message = "Đơn vị tính không được để trống")
    @Schema(description = "Đơn vị tính (kg, con, bó, vỉ, nải, bắp)", example = "con")
    private String donViTinh;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    @Schema(description = "Số lượng nhập ban đầu", example = "25")
    private Integer soLuongTon;

    @NotNull(message = "Hạn sử dụng không được để trống")
    @Future(message = "Hạn sử dụng thực phẩm tươi sống phải ở tương lai")
    @Schema(description = "Hạn sử dụng của thực phẩm", example = "2026-10-15T18:00:00")
    private LocalDateTime hanSuDung;

    @Schema(description = "Nhiệt độ bảo quản khuyến nghị", example = "Bảo quản mát 0 - 4°C")
    private String nhietDoBaoQuan;

    @NotNull(message = "Mã danh mục không được để trống")
    @Schema(description = "Mã danh mục phân loại", example = "3")
    private Integer maDanhMuc;
}
