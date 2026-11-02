package com.cinemasaar.bookings.Service;

import com.cinemasaar.bookings.Enums.BookingStatus;
import com.cinemasaar.bookings.Enums.TicketStatus;
import com.cinemasaar.bookings.Exception.ApiException;
import com.cinemasaar.bookings.Model.BookedSeat;
import com.cinemasaar.bookings.Model.Booking;
import com.cinemasaar.bookings.Model.Ticket;
import com.cinemasaar.bookings.Repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@RequiredArgsConstructor
@Service
public class TicketService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final TicketRepository ticketRepository;
    private final MongoTemplate mongo;

    public Ticket getOrIssue(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return ticketRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                            "Tickets are only issued for confirmed bookings (current: " + booking.getStatus() + ")"));
        }
        return ticketRepository.findByBookingId(booking.getId())
                .orElseGet(() -> issue(booking));
    }

    private Ticket issue(Booking booking) {
        Ticket ticket = Ticket.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .showtimeId(booking.getShowtimeId())
                .seats(booking.getSeats().stream().map(BookedSeat::getSeatId).toList())
                .ticketCode(generateCode())
                .status(TicketStatus.VALID)
                .createdAt(Instant.now())
                .build();
        try {
            return ticketRepository.insert(ticket);
        } catch (DuplicateKeyException e) {
            return ticketRepository.findByBookingId(booking.getId()).orElseThrow(() -> e);
        }
    }

    public Ticket verify(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    public Ticket checkIn(String ticketCode) {
        Ticket updated = mongo.findAndModify(
                Query.query(Criteria.where("ticketCode").is(ticketCode).and("status").is(TicketStatus.VALID)),
                new Update().set("status", TicketStatus.USED).set("usedAt", Instant.now()),
                FindAndModifyOptions.options().returnNew(true),
                Ticket.class);

        if (updated != null) {
            return updated;
        }

        Ticket existing = verify(ticketCode);
        if (existing.getStatus() == TicketStatus.USED) {
            throw new ApiException(HttpStatus.CONFLICT, "Ticket already used at " + existing.getUsedAt());
        }
        throw new ApiException(HttpStatus.GONE, "Ticket was cancelled");
    }

    public void cancelForBooking(String bookingId) {
        mongo.updateFirst(
                Query.query(Criteria.where("bookingId").is(bookingId).and("status").is(TicketStatus.VALID)),
                new Update().set("status", TicketStatus.CANCELLED).set("cancelledAt", Instant.now()),
                Ticket.class);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder("TCK-");
        for (int i = 0; i < 10; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}