package com.ctu.freshgrocery.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class HybridSearchResponse {
    private List<SemanticProductResponse> products;
    @Builder.Default
    private List<SemanticProductResponse> relatedProducts = List.of();
    private String mode;
    @Builder.Default
    private String rankingMethod = "EXACT_NAME_THEN_RRF";
    @Builder.Default
    private int rrfK = 60;
    private float semanticWeight;
    private float keywordWeight;
}
