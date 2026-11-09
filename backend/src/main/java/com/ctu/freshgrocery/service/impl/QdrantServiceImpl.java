package com.ctu.freshgrocery.service.impl;

import com.ctu.freshgrocery.service.QdrantService;
import com.ctu.freshgrocery.exception.AppException;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class QdrantServiceImpl implements QdrantService {

    @Value("${qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${qdrant.port:6333}")
    private int qdrantPort;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestClient restClient;

    private RestClient getClient() {
        if (this.restClient == null) {
            String baseUrl = "http://" + qdrantHost + ":" + qdrantPort;
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(3000);
            factory.setReadTimeout(5000);
            this.restClient = RestClient.builder()
                    .baseUrl(baseUrl)
                    .requestFactory(factory)
                    .build();
        }
        return this.restClient;
    }

    @Override
    public boolean isHealthy() {
        try {
            String res = getClient().get()
                    .uri("/readyz")
                    .retrieve()
                    .body(String.class);
            return res != null && !res.isBlank();
        } catch (Exception e) {
            log.debug("Kiểm tra Qdrant /readyz không thành công: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean createCollectionIfNotExists(String collectionName, int dimension) {
        try {
            // 1. Kiểm tra collection đã tồn tại chưa
            boolean exists = false;
            try {
                getClient().get()
                        .uri("/collections/{name}", collectionName)
                        .retrieve()
                        .body(String.class);
                exists = true;
            } catch (HttpClientErrorException error) {
                if (error.getStatusCode() != HttpStatus.NOT_FOUND) throw error;
            }

            if (exists) {
                log.info("Collection [{}] đã tồn tại trong Qdrant.", collectionName);
                return true;
            }

            log.info("Tạo mới collection [{}] trong Qdrant với dimension={}, metric=Cosine...", collectionName, dimension);
            Map<String, Object> body = Map.of(
                    "vectors", Map.of(
                            "size", dimension,
                            "distance", "Cosine"
                    )
            );

            getClient().put()
                    .uri("/collections/{name}", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("✅ Tạo thành công collection [{}] trên Qdrant!", collectionName);
            return true;
        } catch (Exception e) {
            log.error("Không thể tạo collection [{}] trên Qdrant: {}", collectionName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean upsertPoint(String collectionName, Long pointId, float[] vector, Map<String, Object> payload) {
        QdrantPointDto point = QdrantPointDto.builder()
                .id(pointId)
                .vector(vector)
                .payload(payload)
                .build();
        return upsertBatch(collectionName, List.of(point));
    }

    @Override
    public boolean upsertBatch(String collectionName, List<QdrantPointDto> points) {
        if (points == null || points.isEmpty()) return true;

        try {
            List<Map<String, Object>> pointList = new ArrayList<>();
            for (QdrantPointDto p : points) {
                // Chuyển float[] sang List<Float>
                List<Float> vectorList = new ArrayList<>(p.getVector().length);
                for (float v : p.getVector()) {
                    vectorList.add(v);
                }

                pointList.add(Map.of(
                        "id", p.getId(),
                        "vector", vectorList,
                        "payload", p.getPayload() != null ? p.getPayload() : Collections.emptyMap()
                ));
            }

            Map<String, Object> body = Map.of("points", pointList);

            getClient().put()
                    .uri("/collections/{name}/points?wait=true", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Đã nạp thành công {} vector điểm vào Qdrant collection [{}]", points.size(), collectionName);
            return true;
        } catch (Exception e) {
            log.error("Lỗi khi upsert batch vector vào Qdrant [{}]: {}", collectionName, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deletePoint(String collectionName, Long pointId) {
        try {
            Map<String, Object> body = Map.of("points", List.of(pointId));

            getClient().post()
                    .uri("/collections/{name}/points/delete?wait=true", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Đã xóa pointId={} khỏi Qdrant collection [{}]", pointId, collectionName);
            return true;
        } catch (Exception e) {
            log.error("Lỗi khi xóa vector pointId={} khỏi Qdrant: {}", pointId, e.getMessage());
            return false;
        }
    }

    @Override
    public List<QdrantScoredPoint> search(String collectionName, float[] vector, int limit, Float minScore) {
        try {
            List<Float> vectorList = new ArrayList<>(vector.length);
            for (float v : vector) {
                vectorList.add(v);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("vector", vectorList);
            body.put("limit", limit > 0 ? limit : 5);
            body.put("with_payload", true);
            if (minScore != null && minScore > 0f) {
                body.put("score_threshold", minScore);
            }

            String responseBody = getClient().post()
                    .uri("/collections/{name}/points/search", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("Qdrant returned an empty response");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode resultNode = root.get("result");

            if (resultNode == null || !resultNode.isArray()) {
                throw new IllegalStateException("Invalid Qdrant search response");
            }
            List<QdrantScoredPoint> scoredPoints = new ArrayList<>();
            if (resultNode != null && resultNode.isArray()) {
                for (JsonNode item : resultNode) {
                    Long id = item.get("id").asLong();
                    Float score = (float) item.get("score").asDouble();
                    Map<String, Object> payloadMap = new HashMap<>();
                    if (item.has("payload")) {
                        payloadMap = objectMapper.convertValue(item.get("payload"), Map.class);
                    }
                    scoredPoints.add(QdrantScoredPoint.builder()
                            .id(id)
                            .score(score)
                            .payload(payloadMap)
                            .build());
                }
            }

            return scoredPoints;
        } catch (Exception e) {
            log.warn("Lỗi khi tìm kiếm vector trong Qdrant [{}]: {}", collectionName, e.getMessage());
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Dịch vụ tìm kiếm ngữ nghĩa hiện không khả dụng. Vui lòng thử lại sau.");
        }
    }

    @Override
    public Long getPointsCount(String collectionName) {
        try {
            String res = getClient().get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .body(String.class);

            if (res != null) {
                JsonNode root = objectMapper.readTree(res);
                JsonNode result = root.get("result");
                if (result != null && result.has("points_count")) {
                    return result.get("points_count").asLong();
                }
            }
            return 0L;
        } catch (Exception e) {
            log.debug("Không thể lấy points_count của Qdrant [{}]: {}", collectionName, e.getMessage());
            return 0L;
        }
    }

    @Override
    public String getDashboardUrl() {
        return "http://" + (qdrantHost.equals("qdrant") ? "localhost" : qdrantHost) + ":" + qdrantPort + "/dashboard";
    }
}
