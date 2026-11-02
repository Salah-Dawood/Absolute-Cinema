package com.cinemasaar.bookings.Repository;

import com.cinemasaar.bookings.Enums.PaymentStatus;
import com.cinemasaar.bookings.Model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findFirstByBookingIdAndStatus(String bookingId, PaymentStatus status);
    List<Payment> findByBookingIdOrderByCreatedAtDesc(String bookingId);
}
