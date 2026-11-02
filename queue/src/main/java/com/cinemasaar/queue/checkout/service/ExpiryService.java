package com.cinemasaar.queue.checkout.service;

import com.cinemasaar.queue.admission.service.AdmissionService;
import com.cinemasaar.queue.checkout.model.Booking;
import com.cinemasaar.queue.checkout.model.BookingStatus;
import com.cinemasaar.queue.checkout.repository.BookingRepository;
import com.cinemasaar.queue.payment.model.PaymentIntent;
import com.cinemasaar.queue.payment.model.PaymentStatus;
import com.cinemasaar.queue.payment.repository.PaymentIntentRepository;
import com.cinemasaar.queue.reservation.model.BookingCart;
import com.cinemasaar.queue.reservation.model.CartStatus;
import com.cinemasaar.queue.reservation.model.SeatStatus;
import com.cinemasaar.queue.reservation.model.ShowtimeSeat;
import com.cinemasaar.queue.reservation.repository.BookingCartRepository;
import java.time.Instant;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpiryService {

    private final MongoTemplate mongo;
    private final BookingCartRepository carts;
    private final BookingRepository bookings;
    private final PaymentIntentRepository payments;
    private final AdmissionService admission;

    public ExpiryService(MongoTemplate mongo,
                         BookingCartRepository carts,
                         BookingRepository bookings,
                         PaymentIntentRepository payments,
                         AdmissionService admission) {
        this.mongo = mongo;
        this.carts = carts;
        this.bookings = bookings;
        this.payments = payments;
        this.admission = admission;
    }

    @Transactional
    public boolean expireCart(String cartId) {
        Instant now = Instant.now();
        BookingCart cart = carts.findById(cartId).orElse(null);

        if (cart == null
                || (cart.getStatus() != CartStatus.OPEN
                    && cart.getStatus() != CartStatus.CHECKED_OUT)
                || cart.getExpiresAt().isAfter(now)) {
            return false;
        }

        Booking booking = bookings.findByCartId(cartId).orElse(null);

        // Never pull the rug from under an in-flight payment.
        if (booking != null
                && payments.existsByBookingIdAndStatus(booking.getId(), PaymentStatus.PROCESSING)) {
            return false;
        }

        Query q = Query.query(Criteria.where("_id").is(cartId)
                .and("status").in(CartStatus.OPEN, CartStatus.CHECKED_OUT)
                .and("expiresAt").lte(now));

        if (mongo.updateFirst(q,
                new Update().set("status", CartStatus.EXPIRED).set("expiredAt", now),
                BookingCart.class).getModifiedCount() != 1) {
            return false;
        }

        mongo.updateMulti(
                Query.query(Criteria.where("showtimeId").is(cart.getShowtimeId())
                        .and("cartId").is(cartId)
                        .and("status").is(SeatStatus.HELD)),
                new Update()
                        .set("status", SeatStatus.AVAILABLE)
                        .unset("cartId")
                        .unset("heldBy")
                        .unset("holdExpiresAt")
                        .set("updatedAt", now),
                ShowtimeSeat.class);

        if (booking != null) {
            mongo.updateFirst(
                    Query.query(Criteria.where("_id").is(booking.getId())
                            .and("status").is(BookingStatus.PENDING_PAYMENT)),
                    new Update()
                            .set("status", BookingStatus.EXPIRED)
                            .set("expiredAt", now),
                    Booking.class);

            mongo.updateMulti(
                    Query.query(Criteria.where("bookingId").is(booking.getId())
                            .and("status").in(PaymentStatus.PENDING, PaymentStatus.FAILED)),
                    new Update()
                            .set("status", PaymentStatus.CANCELLED)
                            .set("updatedAt", now),
                    PaymentIntent.class);
        }

        admission.completeByCart(cartId);
        return true;
    }
}
