package com.cinemasaar.queue.payment.dto;

import com.cinemasaar.queue.payment.model.PaymentIntent;
import com.cinemasaar.queue.payment.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String paymentIntentId,
        String bookingId,
        PaymentStatus status,
        BigDecimal amount,
        String reference,
        Instant paidAt
) {

    public static PaymentResponse from(PaymentIntent intent) {
        return new PaymentResponse(
                intent.getId(),
                intent.getBookingId(),
                intent.getStatus(),
                intent.getAmount(),
                intent.getReference(),
                intent.getPaidAt());
    }
}
