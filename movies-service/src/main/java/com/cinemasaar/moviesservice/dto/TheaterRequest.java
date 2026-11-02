package com.cinemasaar.moviesservice.dto;

import jakarta.validation.constraints.*;

// Validates client input before applying catalogue changes.
public record TheaterRequest(@NotBlank @Size(max=200) String name, @NotBlank @Size(max=500) String location) {}
