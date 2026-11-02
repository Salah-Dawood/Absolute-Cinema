package com.cinemasaar.queue.checkout.dto;

import com.cinemasaar.queue.checkout.model.Booking;
import com.cinemasaar.queue.payment.model.PaymentIntent;

public record CheckoutResult(Booking booking, PaymentIntent intent) {
}
