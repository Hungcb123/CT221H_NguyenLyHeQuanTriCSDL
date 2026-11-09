package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.BenchmarkComparisonResponse;

import java.util.Map;

public interface BenchmarkService {
    BenchmarkComparisonResponse compareBenchmark();
    Map<String, Object> generateData(int count);
    Map<String, Object> toggleCoveringIndex(boolean enable);
}
