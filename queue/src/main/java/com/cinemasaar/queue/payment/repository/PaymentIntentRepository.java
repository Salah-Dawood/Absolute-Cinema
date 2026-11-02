package com.cinemasaar.queue.payment.repository;

import com.cinemasaar.queue.payment.model.PaymentIntent;
import com.cinemasaar.queue.payment.model.PaymentStatus;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentIntentRepository extends MongoRepository<PaymentIntent, String> {

    Optional<PaymentIntent> findByIdAndUserId(String id, String userId);

    Optional<PaymentIntent> findByBookingId(String bookingId);

    boolean existsByBookingIdAndStatus(String bookingId, PaymentStatus status);
}
