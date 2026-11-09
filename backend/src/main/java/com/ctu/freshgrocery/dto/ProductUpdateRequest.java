package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
@Schema(description = "Thông tin cập nhật sản phẩm tươi sống")
public class ProductUpdateRequest {

    @Schema(description = "Tên mặt hàng thực phẩm", example = "Cá lóc tươi làm sạch loại 1")
    private String tenSP;

    @Schema(description = "Mô tả chi tiết", example = "Cá lóc đồng làm sạch sẵn đóng khay hút chân không")
    private String moTa;

    @DecimalMin(value = "0.01", message = "Giá bán phải lớn hơn 0 VND")
    @Schema(description = "Giá bán mới (VND). Thử giảm > 50% hoặc tăng > 200% để kích hoạt Trigger bẫy giá bất thường!", example = "35000.00")
    private BigDecimal gia;

    @Schema(description = "Đơn vị tính", example = "con")
    private String donViTinh;

    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    @Schema(description = "Số lượng tồn kho mới. Thử chỉnh về 0 để kích hoạt Trigger bẫy cháy hàng!", example = "0")
    private Integer soLuongTon;

    @Schema(description = "Hạn sử dụng mới. Thử chỉnh cận date <= 1 ngày để kích hoạt Trigger cảnh báo cận date!", example = "2026-10-07T12:00:00")
    private LocalDateTime hanSuDung;

    @Schema(description = "Nhiệt độ bảo quản", example = "Ngăn mát 0 - 2°C")
    private String nhietDoBaoQuan;

    @Schema(description = "Mã danh mục", example = "3")
    private Integer maDanhMuc;
}
