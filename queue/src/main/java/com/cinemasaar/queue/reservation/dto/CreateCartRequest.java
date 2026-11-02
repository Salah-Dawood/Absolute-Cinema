package com.cinemasaar.queue.reservation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCartRequest(@NotBlank String admissionToken) {
}
