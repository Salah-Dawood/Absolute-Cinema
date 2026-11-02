package com.cinemasaar.queue.reservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddonSelection(@NotBlank String addonId, @Min(1) int quantity) {
}
