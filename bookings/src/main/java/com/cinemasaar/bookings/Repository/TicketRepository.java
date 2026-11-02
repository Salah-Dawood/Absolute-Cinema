package com.cinemasaar.bookings.Repository;

import com.cinemasaar.bookings.Model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TicketRepository extends MongoRepository<Ticket, String> {
    Optional<Ticket> findByBookingId(String bookingId);
    Optional<Ticket> findByTicketCode(String ticketCode);
}