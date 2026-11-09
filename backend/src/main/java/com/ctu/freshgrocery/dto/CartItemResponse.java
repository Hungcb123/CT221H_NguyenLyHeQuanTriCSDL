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
@Schema(description = "Thông tin chi tiết một món hàng trong giỏ")
public class CartItemResponse {

    @Schema(description = "Mã sản phẩm", example = "1")
    private Integer maSP;

    @Schema(description = "Tên sản phẩm", example = "Cá lóc tươi làm sạch")
    private String tenSP;

    @Schema(description = "Đường dẫn ảnh sản phẩm")
    private String hinhAnh;

    @Schema(description = "Đơn giá áp dụng (đã trừ khuyến mãi nếu có)", example = "52500.00")
    private BigDecimal donGia;

    @Schema(description = "Giá gốc niêm yết của sản phẩm", example = "75000.00")
    private BigDecimal giaGoc;

    @Schema(description = "Đang áp dụng khuyến mãi hay không", example = "true")
    private Boolean dangKhuyenMai;

    @Schema(description = "Tỷ lệ giảm giá (%)", example = "30.00")
    private BigDecimal phanTramGiam;

    @Schema(description = "Tên chương trình khuyến mãi đang áp dụng", example = "Xả kho thủy hải sản cuối ngày")
    private String tenChuongTrinhKM;

    @Schema(description = "Số tiền tiết kiệm được nhờ khuyến mãi", example = "45000.00")
    private BigDecimal tienTietKiem;

    @Schema(description = "Đơn vị tính", example = "con")
    private String donViTinh;

    @Schema(description = "Số lượng mua trong giỏ", example = "2")
    private Integer soLuong;

    @Schema(description = "Thành tiền", example = "105000.00")
    private BigDecimal thanhTien;

    @Schema(description = "Số lượng còn trong kho", example = "25")
    private Integer soLuongTon;

    @Schema(description = "Hạn sử dụng")
    private LocalDateTime hanSuDung;

    @Schema(description = "Trạng thái đủ hàng (true: Đủ hàng, false: Thiếu hàng)", example = "true")
    private Boolean conDuHang;
}
