package com.cinemasaar.bookings.Model;

import com.cinemasaar.bookings.Enums.NotificationStatus;
import com.cinemasaar.bookings.Enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("notifications")
public class Notification {
    @Id
    private String id;
    private String userId;
    private String bookingId;
    private NotificationType type;
    private String recipientEmail;
    private String subject;
    private NotificationStatus status;
    private String errorMessage;
    private Instant sentAt;
    private Instant createdAt;
}