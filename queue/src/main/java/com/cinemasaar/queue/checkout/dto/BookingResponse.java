package com.cinemasaar.queue.checkout.dto;

import com.cinemasaar.queue.checkout.model.Booking;
import com.cinemasaar.queue.checkout.model.BookingStatus;
import com.cinemasaar.queue.payment.model.PaymentIntent;
import com.cinemasaar.queue.payment.model.PaymentStatus;
import com.cinemasaar.queue.reservation.dto.CartResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingResponse(
        String bookingId,
        String cartId,
        BookingStatus status,
        List<CartResponse.Item> seats,
        BigDecimal seatTotal,
        BigDecimal addonTotal,
        BigDecimal total,
        Instant expiresAt,
        Instant confirmedAt,
        PaymentSummary payment
) {

    public static BookingResponse from(Booking booking, PaymentIntent payment) {
        List<CartResponse.Item> seats = booking.getSeats() == null
                ? List.of()
                : booking.getSeats().stream().map(CartResponse.Item::from).toList();

        PaymentSummary summary = payment == null
                ? null
                : new PaymentSummary(
                        payment.getId(),
                        payment.getStatus(),
                        payment.getReference(),
                        payment.getPaidAt());

        return new BookingResponse(
                booking.getId(),
                booking.getCartId(),
                booking.getStatus(),
                seats,
                booking.getSeatTotal(),
                booking.getAddonTotal(),
                booking.getTotal(),
                booking.getExpiresAt(),
                booking.getConfirmedAt(),
                summary);
    }

    public record PaymentSummary(
            String paymentIntentId,
            PaymentStatus status,
            String reference,
            Instant paidAt
    ) {
    }
}
