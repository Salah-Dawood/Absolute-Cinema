package com.cinemasaar.moviesservice.dto;

import jakarta.validation.constraints.*;

// Validates client input before applying catalogue changes.
public record SeatRequest(@NotBlank String screenId, @NotBlank @Pattern(regexp="[A-Za-z]{1,3}") String rowLabel, @NotNull @Min(1) @Max(1000) Integer seatNumber) {}
