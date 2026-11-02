package com.cinemasaar.queue.checkout.repository;

import com.cinemasaar.queue.checkout.model.Booking;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookingRepository extends MongoRepository<Booking, String> {

    Optional<Booking> findByIdAndUserId(String id, String userId);

    Optional<Booking> findByCartId(String cartId);
}
