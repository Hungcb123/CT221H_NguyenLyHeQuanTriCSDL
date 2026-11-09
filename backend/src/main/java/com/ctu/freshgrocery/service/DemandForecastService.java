package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.BatchForecastResponse;
import com.ctu.freshgrocery.dto.ForecastCustomRequest;
import com.ctu.freshgrocery.dto.ForecastResponse;

import java.util.List;

public interface DemandForecastService {
    ForecastResponse predictForProduct(Integer productId);
    ForecastResponse predictCustom(ForecastCustomRequest request);
    BatchForecastResponse runBatchForecast();
    List<ForecastResponse> getForecastHistory(Integer productId);
}
