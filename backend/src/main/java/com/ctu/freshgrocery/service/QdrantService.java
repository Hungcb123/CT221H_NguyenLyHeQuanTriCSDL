package com.ctu.freshgrocery.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public interface QdrantService {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class QdrantScoredPoint {
        private Long id;
        private Float score;
        private Map<String, Object> payload;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class QdrantPointDto {
        private Long id;
        private float[] vector;
        private Map<String, Object> payload;
    }

    boolean isHealthy();

    boolean createCollectionIfNotExists(String collectionName, int dimension);

    boolean upsertPoint(String collectionName, Long pointId, float[] vector, Map<String, Object> payload);

    boolean upsertBatch(String collectionName, List<QdrantPointDto> points);

    boolean deletePoint(String collectionName, Long pointId);

    List<QdrantScoredPoint> search(String collectionName, float[] vector, int limit, Float minScore);

    Long getPointsCount(String collectionName);

    String getDashboardUrl();
}
