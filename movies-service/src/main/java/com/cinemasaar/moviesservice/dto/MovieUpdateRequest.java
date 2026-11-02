package com.cinemasaar.moviesservice.dto;

import jakarta.validation.constraints.*;

// Validates client input before applying catalogue changes.
public record MovieUpdateRequest(@NotBlank @Size(max=500) String title, @Size(max=100) String rated, @Size(max=500) String genre, @Size(max=500) String language, @Size(max=10000) String plot, @NotNull Boolean isShowing) {}
