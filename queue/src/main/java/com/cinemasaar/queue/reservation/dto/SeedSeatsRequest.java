package com.cinemasaar.queue.reservation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record SeedSeatsRequest(
        @NotEmpty List<String> rows,
        @Min(1) @Max(50) int seatsPerRow,
        @NotNull @DecimalMin("0.01") BigDecimal price
) {
}
