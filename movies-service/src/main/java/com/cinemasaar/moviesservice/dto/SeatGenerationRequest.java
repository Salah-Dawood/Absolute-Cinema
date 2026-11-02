package com.cinemasaar.moviesservice.dto;

import jakarta.validation.constraints.*;

// Validates client input before applying catalogue changes.
public record SeatGenerationRequest(@NotBlank String screenId, @NotEmpty @Size(max=26) java.util.List<@NotBlank @Pattern(regexp="[A-Za-z]{1,3}") String> rows, @Min(1) @Max(100) int seatsPerRow) {}
