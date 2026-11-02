package com.cinemasaar.moviesservice.dto;

import jakarta.validation.constraints.*;

// Validates client input before applying catalogue changes.
public record ScreenRequest(@NotBlank String theaterId, @NotBlank @Size(max=100) String name) {}
