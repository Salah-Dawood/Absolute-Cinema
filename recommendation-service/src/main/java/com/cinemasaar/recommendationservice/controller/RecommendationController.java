package com.cinemasaar.recommendationservice.controller;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.cinemasaar.recommendationservice.dto.*;
import com.cinemasaar.recommendationservice.service.RecommendationService;

// Returns recommendations without asserting bookable seat availability.
@RestController @RequiredArgsConstructor @RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService service;
    @PostMapping public RecommendationResponse recommend(@Valid @RequestBody PreferencesRequest request) { return service.recommend(request); }
}
