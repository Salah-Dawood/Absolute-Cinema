package com.cinemasaar.moviesservice.dto;

import com.fasterxml.jackson.annotation.*;
import java.util.List;

// Returns title matches and pagination information without importing anything.
@JsonIgnoreProperties(ignoreUnknown = true)
public record OmdbSearchResponse(@JsonProperty("Search") List<OmdbMovieResponse> results,
        String totalResults, @JsonProperty("Response") String response, @JsonProperty("Error") String error) {}
