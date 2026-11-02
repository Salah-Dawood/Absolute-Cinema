package com.cinemasaar.bookings.dto.Response;

import com.cinemasaar.bookings.Enums.TicketStatus;
import com.cinemasaar.bookings.Model.Ticket;

import java.time.Instant;
import java.util.List;

public record TicketResponse(
        String ticketCode,
        String bookingId,
        String showtimeId,
        List<String> seats,
        TicketStatus status,
        Instant createdAt,
        Instant usedAt,
        String qrCode
) {
    public static TicketResponse from(Ticket t, String qrCode) {
        return new TicketResponse(t.getTicketCode(), t.getBookingId(), t.getShowtimeId(),
                t.getSeats(), t.getStatus(), t.getCreatedAt(), t.getUsedAt(), qrCode);
    }
}