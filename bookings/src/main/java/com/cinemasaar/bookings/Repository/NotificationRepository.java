package com.cinemasaar.bookings.Repository;

import com.cinemasaar.bookings.Enums.NotificationStatus;
import com.cinemasaar.bookings.Enums.NotificationType;
import com.cinemasaar.bookings.Model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    Optional<Notification> findFirstByBookingIdAndTypeAndStatusOrderByCreatedAtDesc(
            String bookingId, NotificationType type, NotificationStatus status);

    List<Notification> findByBookingIdOrderByCreatedAtDesc(String bookingId);
}