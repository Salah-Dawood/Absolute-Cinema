package com.cinemasaar.recommendationservice.dto;

import java.util.List;

// Strict OpenAI output contains catalogue IDs and short reasons only.
public record AiSelection(List<Item> recommendations) {
    public record Item(String movieId, String explanation) {}
}
