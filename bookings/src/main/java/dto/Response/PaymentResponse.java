package dto.Response;

import Enums.BookingStatus;
import Enums.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponse(String bookingId, BookingStatus bookingStatus,
                              String paymentId, PaymentStatus paymentStatus,
                              BigDecimal amount, String message) {
}
