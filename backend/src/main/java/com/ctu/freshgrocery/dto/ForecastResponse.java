package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả dự báo nhu cầu & khuyến nghị nhập hàng tươi sống từ AI")
public class ForecastResponse {

    @Schema(description = "Mã bản ghi dự báo đã lưu vào CSDL (bảng DuBaoNhapHang)", example = "1")
    private Long maDuBao;

    @Schema(description = "Mã sản phẩm", example = "1")
    private Integer maSP;

    @Schema(description = "Tên sản phẩm", example = "Cá lóc tươi làm sạch")
    private String tenSP;

    @Schema(description = "Đơn vị tính", example = "con")
    private String donViTinh;

    @Schema(description = "Ngày dự báo tiêu thụ", example = "2026-10-15")
    private LocalDate ngayDuBao;

    @Schema(description = "Số lượng khách dự kiến tiêu thụ (đơn vị sản phẩm)", example = "40.57")
    private Double soLuongDuBaoTieuThu;

    @Schema(description = "Số lượng tồn kho tại thời điểm dự báo", example = "15")
    private Integer soLuongTonHienTai;

    @Schema(description = "Khuyến nghị số lượng cần nhập thêm (Buffer an toàn 10%)", example = "30")
    private Integer soLuongDeXuatNhap;

    @Schema(description = "Độ tin cậy đã hiệu chỉnh; null nếu mô hình chưa có phép hiệu chỉnh")
    private Double doTinCay;

    @Schema(description = "Mô hình và kiến trúc thực thi", example = "PyTorch-MLP-ONNX")
    private String thuatToan;

    @Schema(description = "Mức độ rủi ro hư hỏng / ế hàng (THAP, TRUNG_BINH, CAO)", example = "THAP")
    private String ruiRoHuHong;

    @Schema(description = "Thời gian thực thi suy luận trực tiếp trên Java JVM (mili-giây)", example = "2")
    private Long doTreInferenceMs;

    @Schema(description = "Thông báo liên kết nhật ký cảnh báo (nếu rủi ro ở mức CAO)")
    private String canhBaoLienKet;

    @Schema(description = "Vector 10 đặc trưng chuỗi thời gian đã dùng để suy luận")
    private Map<String, Object> featuresUsed;
}
