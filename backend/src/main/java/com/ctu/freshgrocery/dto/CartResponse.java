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
@Schema(description = "Thông tin giỏ hàng của khách hàng")
public class CartResponse {

    @Schema(description = "Mã giỏ hàng", example = "1")
    private Integer maGioHang;

    @Schema(description = "Mã khách hàng", example = "1")
    private Integer maKH;

    @Schema(description = "Tổng tiền tạm tính của giỏ hàng", example = "182000.00")
    private BigDecimal tongTien;

    @Schema(description = "Tổng số lượng mặt hàng trong giỏ", example = "2")
    private Integer tongSoLuong;

    @Schema(description = "Tổng số tiền tiết kiệm được từ các mặt hàng khuyến mãi", example = "45000.00")
    private BigDecimal tongTienTietKiem;

    @Schema(description = "Danh sách sản phẩm trong giỏ")
    private List<CartItemResponse> items;
}
