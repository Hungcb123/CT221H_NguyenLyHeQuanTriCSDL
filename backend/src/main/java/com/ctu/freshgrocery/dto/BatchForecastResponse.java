package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả dự báo hàng loạt cho toàn bộ danh mục thực phẩm tươi")
public class BatchForecastResponse {

    @Schema(description = "Ngày áp dụng dự báo", example = "2026-10-15")
    private LocalDate ngayDuBao;

    @Schema(description = "Tổng số mặt hàng đã chạy dự báo", example = "12")
    private Integer tongSoMatHang;

    @Schema(description = "Tổng số lượng hàng tươi sống khuyến nghị nhập thêm cho toàn hệ thống", example = "285")
    private Integer tongSoLuongDeXuatNhap;

    @Schema(description = "Số mặt hàng phát hiện nguy cơ hư hỏng / ế hàng ở mức CAO", example = "1")
    private Integer soMatHangRuiRoCao;

    @Schema(description = "Danh sách chi tiết kết quả dự báo từng mặt hàng")
    private List<ForecastResponse> danhSachDuBao;
}
