package com.cinemasaar.moviesservice.dto;

import jakarta.validation.constraints.*;

// Validates client input before applying catalogue changes.
public record ShowingRequest(@NotNull Boolean isShowing) {}
