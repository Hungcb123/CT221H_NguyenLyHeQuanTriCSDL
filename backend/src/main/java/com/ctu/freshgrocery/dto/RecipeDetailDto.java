package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chi tiết công thức món ăn và sự sẵn có của nguyên liệu trong kho")
public class RecipeDetailDto {

    @Schema(description = "Mã công thức món ăn", example = "1")
    private Integer maCongThuc;

    @Schema(description = "Tên món ăn", example = "Canh chua cá lóc miền Tây")
    private String tenMonAn;

    @Schema(description = "Mô tả hương vị món ăn")
    private String moTa;

    @Schema(description = "Khẩu phần (số người ăn)", example = "4")
    private Integer khauPhan;

    @Schema(description = "Hướng dẫn các bước nấu ăn")
    private String huongDanNau;

    @Schema(description = "Tổng chi phí ước tính mua nguyên liệu tươi sống (VND)", example = "128000.00")
    private BigDecimal tongTienUocTinh;

    @Schema(description = "Tất cả nguyên liệu có sẵn đủ trong kho để nấu không?", example = "true")
    private Boolean duNguyenLieuTrongKho;

    @Schema(description = "Danh sách chi tiết từng nguyên liệu tươi sống")
    private List<IngredientItemDto> nguyenLieuList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientItemDto {
        private Integer maSP;
        private String tenSP;
        private BigDecimal soLuongDinhLuong;
        private String donViTinh;
        private BigDecimal donGia;
        private BigDecimal thanhTien;
        private Integer tonKhoHienTai;
        private Boolean conHang;
    }
}
