package com.cinemasaar.queue.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AddonSelectionRequest(@NotNull @Valid List<AddonSelection> items) {
}
