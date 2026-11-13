package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chi tiết hóa đơn đơn hàng")
public class OrderDetailResponse {

    @Schema(description = "Mã đơn hàng", example = "1")
    private Integer maDH;

    @Schema(description = "Mã khách hàng", example = "1")
    private Integer maKH;

    @Schema(description = "Tên khách hàng", example = "Nguyễn Văn An")
    private String tenKhachHang;

    @Schema(description = "Số điện thoại", example = "0901234567")
    private String soDienThoai;

    @Schema(description = "Địa chỉ giao hàng", example = "123 Đường 3/2, Phường Xuân Khánh, Cần Thơ")
    private String diaChiGiaoHang;

    @Schema(description = "Ghi chú giao hàng")
    private String ghiChu;

    @Schema(description = "Thời gian tạo đơn")
    private LocalDateTime ngayTao;

    @Schema(description = "Tổng tiền (VND)", example = "182000.00")
    private BigDecimal tongTien;

    @Schema(description = "Trạng thái đơn hàng", example = "PAID")
    private String trangThai;
    private String phuongThucThanhToan;

    private String nguonDon;

    @Schema(description = "Danh sách sản phẩm trong hóa đơn")
    private List<OrderItemDetailDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Mặt hàng trong hóa đơn")
    public static class OrderItemDetailDto {
        private Integer maSP;
        private String tenSP;
        private Integer soLuong;
        private BigDecimal donGia;
        private BigDecimal giaGoc;
        private BigDecimal tienGiam;
        private Integer maKM;
        private BigDecimal thanhTien;
    }
}
