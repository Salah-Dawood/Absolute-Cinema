package model;

import Enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Document("bookings")
public class Booking {

    @Id
    private String id;
    private String userId;
    private String showtimeId;
    private String seatHoldId;
    private List<BookingSeat> seats;
    private List<BookingAddon> addons;
    private BigDecimal seatsTotal;
    private BigDecimal addonsTotal;
    private BigDecimal totalAmount;
    private BookingStatus status;
    //seat hold expiry
    private Instant expiresAt;
    //set when payment succeeds
    private Instant confirmedAt;
    //set on admin cancellation
    private Instant cancelledAt;
    private String cancellationReason;
    //the admin who canceled it
    private String cancelledByUserId;
    private Instant createdAt;
    private Instant updatedAt;
}
