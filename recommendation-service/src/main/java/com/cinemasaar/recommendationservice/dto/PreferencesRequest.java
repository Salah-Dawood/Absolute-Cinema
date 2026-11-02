package com.cinemasaar.recommendationservice.dto;

import jakarta.validation.constraints.*;
import java.util.List;

// Optional preferences guide ranking; limit defaults to three.
public record PreferencesRequest(@Size(max=100) String mood,
        @Size(max=10) List<@NotBlank @Size(max=60) String> genres,
        @Size(max=100) String language, @Size(max=100) String energyLevel,
        @Min(1) @Max(600) Integer maxDurationMinutes, @Size(max=2000) String description,
        @Min(1) @Max(5) Integer limit) {
    public int resultLimit() { return limit == null ? 3 : limit; }
}
