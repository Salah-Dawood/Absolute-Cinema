package com.cinemasaar.bookings.Service;

import com.cinemasaar.bookings.Enums.AuditAction;
import com.cinemasaar.bookings.Enums.BookingStatus;
import com.cinemasaar.bookings.Exception.ApiException;
import com.cinemasaar.bookings.Model.Booking;
import com.cinemasaar.bookings.Repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class CancellationService {

    private static final String BOOKINGS = "bookings";
    private static final String SEATS = "showtime_seats";
    private static final String PAYMENTS = "payment_intents";

    private final BookingRepository bookingRepository;
    private final MongoTemplate mongo;
    private final TicketService ticketService;
    private final AuditService auditService;

    @Transactional
    public Booking cancelByAdmin(String bookingId, String adminUserId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only confirmed bookings can be cancelled (current: " + booking.getStatus() + ")");
        }

        Instant now = Instant.now();

        long changed = mongo.updateFirst(
                Query.query(Criteria.where("_id").is(bookingId)
                        .and("status").is(BookingStatus.CONFIRMED.name())),
                new Update()
                        .set("status", BookingStatus.CANCELLED.name())
                        .set("cancelledAt", now)
                        .set("cancellationReason", reason)
                        .set("cancelledByUserId", adminUserId),
                BOOKINGS).getModifiedCount();

        if (changed != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Booking was changed by someone else. Please refresh.");
        }

        //Seats BOOKED -> AVAILABLE so others can buy them
        long releasedSeats = mongo.updateMulti(
                Query.query(Criteria.where("bookingId").is(bookingId).and("status").is("BOOKED")),
                new Update()
                        .set("status", "AVAILABLE")
                        .set("updatedAt", now)
                        .unset("cartId")
                        .unset("heldBy")
                        .unset("bookingId")
                        .unset("holdExpiresAt"),
                SEATS).getModifiedCount();

        //Payment PAID -> REFUND_REQUIRED
        mongo.updateFirst(
                Query.query(Criteria.where("bookingId").is(bookingId).and("status").is("PAID")),
                new Update()
                        .set("status", "REFUND_REQUIRED")
                        .set("failureReason", "Booking cancelled by admin: " + reason)
                        .set("updatedAt", now),
                PAYMENTS);

        //Ticket -> CANCELLED
        ticketService.cancelForBooking(bookingId);

        auditService.log(adminUserId, AuditAction.BOOKING_CANCELLED, "BOOKING", bookingId, reason,
                "Released " + releasedSeats + " seat(s); payment marked REFUND_REQUIRED");

        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
    }
}