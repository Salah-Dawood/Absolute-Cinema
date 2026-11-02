package com.cinemasaar.bookings.Repository;

import com.cinemasaar.bookings.Enums.BookingStatus;
import com.cinemasaar.bookings.Model.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);
}