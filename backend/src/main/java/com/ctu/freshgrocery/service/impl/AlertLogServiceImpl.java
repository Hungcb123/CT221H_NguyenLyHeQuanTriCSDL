package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.dto.AlertLogResponse;
import com.ctu.freshgrocery.dto.ApiResponse;
import com.ctu.freshgrocery.model.AlertLog;
import com.ctu.freshgrocery.repository.AlertLogRepository;
import com.ctu.freshgrocery.service.AlertLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertLogServiceImpl implements AlertLogService {

    private final AlertLogRepository alertLogRepository;

    @Override
    public ApiResponse<List<AlertLogResponse>> getRecentAlerts() {
        List<AlertLogResponse> dtos = alertLogRepository.findTop20ByOrderByNgayGhiDesc()
                .stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.ok("Lấy danh sách 20 cảnh báo mới nhất từ Trigger thành công", dtos);
    }

    @Override
    public ApiResponse<List<AlertLogResponse>> getAlertsByProduct(Integer productId) {
        List<AlertLogResponse> dtos = alertLogRepository.findByMaSPOrderByNgayGhiDesc(productId)
                .stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.ok("Lấy lịch sử cảnh báo của sản phẩm ID=" + productId + " thành công", dtos);
    }

    @Override
    public ApiResponse<List<AlertLogResponse>> getAlertsByEventType(String eventType) {
        List<AlertLogResponse> dtos = alertLogRepository.findByLoaiSuKienOrderByNgayGhiDesc(eventType)
                .stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.ok("Lấy danh sách cảnh báo theo sự kiện [" + eventType + "] thành công", dtos);
    }

    private AlertLogResponse toDto(AlertLog entity) {
        return AlertLogResponse.builder()
                .maLog(entity.getMaLog())
                .loaiSuKien(entity.getLoaiSuKien())
                .noiDung(entity.getNoiDung())
                .maSP(entity.getMaSP())
                .nguoiThucHien(entity.getNguoiThucHien())
                .ngayGhi(entity.getNgayGhi())
                .build();
    }
}
