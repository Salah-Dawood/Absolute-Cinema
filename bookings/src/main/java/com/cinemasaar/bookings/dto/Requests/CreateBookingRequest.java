package com.cinemasaar.bookings.dto.Requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateBookingRequest(@NotBlank String seatHoldId, @Valid List<AddonSelection> addons) {
}
