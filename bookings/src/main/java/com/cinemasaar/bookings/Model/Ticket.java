package com.cinemasaar.bookings.Model;

import com.cinemasaar.bookings.Enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("tickets")
public class Ticket {
    @Id
    private String id;

    //one ticket per booking
    @Indexed(unique = true)
    private String bookingId;

    //what the QR code contains
    @Indexed(unique = true)
    private String ticketCode;

    private String userId;
    private String showtimeId;
    private List<String> seats;
    private TicketStatus status;
    private Instant createdAt;
    private Instant usedAt;
    private Instant cancelledAt;
}