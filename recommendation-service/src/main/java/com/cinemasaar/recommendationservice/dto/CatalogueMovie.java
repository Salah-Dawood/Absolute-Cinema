package com.cinemasaar.recommendationservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Uses only catalogue fields needed to choose movies; it makes no availability claims.
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogueMovie(String id, String title, String genre, String language, String runtime,
        String plot, Boolean isShowing) {}
