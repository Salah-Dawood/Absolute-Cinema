package com.cinemasaar.bookings.dto.Response;

import com.cinemasaar.bookings.Enums.BookingStatus;
import com.cinemasaar.bookings.Enums.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponse(String bookingId, BookingStatus bookingStatus,
                              String paymentId, PaymentStatus paymentStatus,
                              BigDecimal amount, String message) {
}
