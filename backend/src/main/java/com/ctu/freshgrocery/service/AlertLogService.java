package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.AlertLogResponse;
import com.ctu.freshgrocery.dto.ApiResponse;

import java.util.List;

public interface AlertLogService {
    ApiResponse<List<AlertLogResponse>> getRecentAlerts();
    ApiResponse<List<AlertLogResponse>> getAlertsByProduct(Integer productId);
    ApiResponse<List<AlertLogResponse>> getAlertsByEventType(String eventType);
}
