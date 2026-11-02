package com.cinemasaar.recommendationservice.dto;

import java.util.List;

// Identifies fallback output so callers can explain the source of recommendations.
public record RecommendationResponse(String source, List<Item> recommendations) {
    public record Item(CatalogueMovie movie, String explanation) {}
}
