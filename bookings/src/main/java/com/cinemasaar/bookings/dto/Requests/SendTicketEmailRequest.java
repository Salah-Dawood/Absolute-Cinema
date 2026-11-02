package com.cinemasaar.bookings.dto.Requests;

import jakarta.validation.constraints.Email;

public record SendTicketEmailRequest(@Email String email) {
}