package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chương trình khuyến mãi / Flash Sale")
public class PromotionResponse {

    @Schema(description = "Mã chương trình", example = "1")
    private Integer maKM;

    @Schema(description = "Tên chương trình", example = "Flash Sale Giờ Vàng Thủy Hải Sản")
    private String tenKM;

    @Schema(description = "Mô tả chi tiết", example = "Ưu đãi sốc giảm 20% cho các loại hải sản tươi sống")
    private String moTa;

    @Schema(description = "Loại chương trình: FLASH_SALE, XA_KHO_CAN_DATE, GIAM_THEO_MUA, TRI_AN_KHACH_HANG", example = "FLASH_SALE")
    private String loaiKM;

    @Schema(description = "Thời gian bắt đầu")
    private LocalDateTime ngayBatDau;

    @Schema(description = "Thời gian kết thúc")
    private LocalDateTime ngayKetThuc;

    @Schema(description = "Trạng thái hoạt động", example = "true")
    private Boolean trangThai;

    @Schema(description = "Chương trình hiện tại có đang trong hiệu lực thời gian hay không", example = "true")
    private Boolean dangHieuLuc;

    @Schema(description = "Số lượng sản phẩm áp dụng ưu đãi trong chương trình", example = "3")
    private Integer itemsCount;

    @Schema(description = "Số lượng sản phẩm áp dụng ưu đãi trong chương trình", example = "3")
    private Integer soLuongSanPham;

    @Schema(description = "Danh sách chi tiết sản phẩm khuyến mãi")
    private List<PromotionDetailResponse> items;
}
