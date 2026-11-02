package com.cinemasaar.bookings.Model;

import com.cinemasaar.bookings.Enums.BookingStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@Document("bookings")
public class Booking {
    @Id
    private String id;
    private String cartId;
    private String userId;
    private String showtimeId;
    private String paymentIntentId;
    private BookingStatus status;
    private List<BookedSeat> seats;
    private BigDecimal seatTotal;
    private BigDecimal addonTotal;
    private BigDecimal total;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant confirmedAt;
    private Instant expiredAt;

    private Instant cancelledAt;
    private String cancellationReason;
    private String cancelledByUserId;
}