package com.ctu.freshgrocery.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductTypeRequest {
    private Integer maSP;
    @NotBlank @Size(max = 80) private String tenLoai;
    @NotNull @DecimalMin("0.01") private BigDecimal gia;
    @NotBlank @Size(max = 20) private String donViTinh;
    @NotNull @Min(0) private Integer soLuongTon;
    @NotNull private LocalDateTime hanSuDung;
    @Size(max = 50) private String nhietDoBaoQuan;
    @Size(max = 1000)
    @Pattern(regexp = "^(?:https?://[^\\s]+|/(?!/)[^\\s]*)?$")
    private String hinhAnh;
    private String moTa;
    private boolean trangThaiXoa;
}
