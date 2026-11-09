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
@Schema(description = "Thông tin sản phẩm từ kết quả tìm kiếm ngữ nghĩa (Semantic Vector Search)")
public class SemanticProductResponse {

    @Schema(description = "Mã sản phẩm", example = "1")
    private Integer maSP;

    @Schema(description = "Tên sản phẩm tươi sống", example = "Cá lóc tươi làm sạch")
    private String tenSP;

    @Schema(description = "Mô tả sản phẩm", example = "Cá lóc đồng làm sạch mang và ruột, thích hợp nấu canh chua hoặc kho tộ")
    private String moTa;

    @Schema(description = "Giá bán niêm yết (VND)", example = "75000.00")
    private BigDecimal gia;

    @Schema(description = "Giá gốc niêm yết", example = "75000.00")
    private BigDecimal giaGoc;

    @Schema(description = "Giá hiện tại đang bán (đã trừ khuyến mãi nếu có)", example = "52500.00")
    private BigDecimal giaHienTai;

    @Schema(description = "Trạng thái có đang trong chương trình khuyến mãi không", example = "true")
    private Boolean dangKhuyenMai;

    @Schema(description = "Phần trăm giảm giá (%)", example = "30.00")
    private BigDecimal phanTramGiam;

    @Schema(description = "Tên chương trình khuyến mãi", example = "Flash Sale Hải Sản Tươi Sống")
    private String tenChuongTrinhKM;

    @Schema(description = "Mã chương trình khuyến mãi", example = "1")
    private Integer maKM;

    @Schema(description = "Đơn vị tính", example = "con")
    private String donViTinh;

    @Schema(description = "Số lượng tồn kho", example = "15")
    private Integer soLuongTon;

    @Schema(description = "Hạn sử dụng")
    private LocalDateTime hanSuDung;

    @Schema(description = "Nhiệt độ bảo quản", example = "Ngăn mát 0 - 2°C")
    private String nhietDoBaoQuan;

    @Schema(description = "Mã danh mục", example = "3")
    private Integer maDanhMuc;

    @Schema(description = "Tên danh mục", example = "Thủy hải sản tươi")
    private String tenDanhMuc;

    @Schema(description = "Điểm số tương đồng Cosine (0.0 đến 1.0)", example = "0.852")
    private Float similarityScore;

    @Schema(description = "Điểm BM25 trên tên và mô tả; không chuẩn hóa về 0–1")
    private Float keywordScore;

    @Schema(description = "Điểm RRF kết hợp thứ hạng BM25 và vector; không phải xác suất hoặc Cosine")
    private Float rankingScore;

    @Schema(description = "Cơ chế khớp kết quả: SEMANTIC_VECTOR (Tìm theo ngữ nghĩa AI) hoặc EXACT_KEYWORD (Khớp từ khóa SQL)", example = "SEMANTIC_VECTOR")
    private String matchType;
}
