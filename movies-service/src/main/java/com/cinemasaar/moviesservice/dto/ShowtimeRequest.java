package com.cinemasaar.moviesservice.dto;

import jakarta.validation.constraints.*;

// Validates client input before applying catalogue changes.
public record ShowtimeRequest(@NotBlank String movieId, @NotBlank String screenId, @NotNull java.time.Instant startsAt, @NotNull java.time.Instant endsAt, @NotNull @DecimalMin("0.00") @Digits(integer=10, fraction=2) java.math.BigDecimal basePrice) {}
