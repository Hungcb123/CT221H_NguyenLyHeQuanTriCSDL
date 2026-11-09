package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu tạo mới chương trình khuyến mãi / Flash Sale")
public class PromotionCreateRequest {

    @NotBlank(message = "Tên chương trình khuyến mãi không được để trống")
    @Schema(description = "Tên chương trình", example = "Flash Sale Giờ Vàng Thủy Hải Sản")
    @jakarta.validation.constraints.Size(max=150)
    private String tenKM;

    @Schema(description = "Mô tả chi tiết chương trình", example = "Ưu đãi sốc giảm 20% cho các loại hải sản tươi sống")
    @jakarta.validation.constraints.Size(max=500)
    private String moTa;

    @NotBlank(message = "Loại khuyến mãi không được để trống")
    @Schema(description = "Loại chương trình: FLASH_SALE, XA_KHO_CAN_DATE, GIAM_THEO_MUA, TRI_AN_KHACH_HANG", example = "FLASH_SALE")
    @jakarta.validation.constraints.Pattern(regexp="FLASH_SALE|XA_KHO_CAN_DATE|GIAM_THEO_MUA|TRI_AN_KHACH_HANG")
    private String loaiKM;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @Schema(description = "Thời gian bắt đầu hiệu lực")
    private LocalDateTime ngayBatDau;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @Schema(description = "Thời gian kết thúc hiệu lực")
    private LocalDateTime ngayKetThuc;
}
