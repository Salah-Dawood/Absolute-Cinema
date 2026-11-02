package com.cinemasaar.queue.reservation.repository;

import com.cinemasaar.queue.reservation.model.BookingCart;
import com.cinemasaar.queue.reservation.model.CartStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BookingCartRepository extends MongoRepository<BookingCart, String> {

    Optional<BookingCart> findByIdAndUserId(String id, String userId);

    List<BookingCart> findTop100ByStatusInAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            Collection<CartStatus> statuses, Instant now);
}
