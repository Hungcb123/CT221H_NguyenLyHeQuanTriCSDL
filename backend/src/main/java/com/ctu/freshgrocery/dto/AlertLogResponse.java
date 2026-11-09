package com.ctu.freshgrocery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Nhật ký cảnh báo do Trigger SQL Server tự động bẫy sự kiện")
public class AlertLogResponse {
    private Long maLog;
    private String loaiSuKien;
    private String noiDung;
    private Integer maSP;
    private String nguoiThucHien;
    private LocalDateTime ngayGhi;
}
