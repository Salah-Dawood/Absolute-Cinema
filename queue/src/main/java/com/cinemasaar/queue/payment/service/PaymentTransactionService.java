package com.cinemasaar.queue.payment.service;

import com.cinemasaar.queue.admission.service.AdmissionService;
import com.cinemasaar.queue.checkout.model.Booking;
import com.cinemasaar.queue.checkout.model.BookingStatus;
import com.cinemasaar.queue.checkout.repository.BookingRepository;
import com.cinemasaar.queue.common.exception.NotFoundException;
import com.cinemasaar.queue.payment.exception.PaymentException;
import com.cinemasaar.queue.payment.model.PaymentIntent;
import com.cinemasaar.queue.payment.model.PaymentStatus;
import com.cinemasaar.queue.payment.repository.PaymentIntentRepository;
import com.cinemasaar.queue.reservation.exception.CartExpiredException;
import com.cinemasaar.queue.reservation.model.BookingCart;
import com.cinemasaar.queue.reservation.model.CartStatus;
import com.cinemasaar.queue.reservation.model.SeatStatus;
import com.cinemasaar.queue.reservation.model.ShowtimeSeat;
import java.time.Instant;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentTransactionService {

    private final MongoTemplate mongo;
    private final PaymentIntentRepository payments;
    private final BookingRepository bookings;
    private final AdmissionService admission;

    public PaymentTransactionService(MongoTemplate mongo,
                                     PaymentIntentRepository payments,
                                     BookingRepository bookings,
                                     AdmissionService admission) {
        this.mongo = mongo;
        this.payments = payments;
        this.bookings = bookings;
        this.admission = admission;
    }

    @Transactional
    public PaymentIntent claim(String intentId, String userId,
                               String idempotencyKey, String method) {
        PaymentIntent intent = payments.findByIdAndUserId(intentId, userId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (intent.getStatus() == PaymentStatus.PAID) {
            return intent;
        }
        if (intent.getStatus() == PaymentStatus.PROCESSING) {
            throw new PaymentException("PAYMENT_IN_PROGRESS",
                    "Payment is already being processed");
        }
        if (intent.getStatus() != PaymentStatus.PENDING
                && intent.getStatus() != PaymentStatus.FAILED) {
            throw new PaymentException("PAYMENT_NOT_PAYABLE",
                    "Payment is " + intent.getStatus());
        }

        Booking booking = bookings.findById(intent.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new PaymentException("BOOKING_NOT_PAYABLE",
                    "Booking is " + booking.getStatus());
        }
        if (!booking.getExpiresAt().isAfter(Instant.now())) {
            throw new CartExpiredException();
        }

        Query q = Query.query(Criteria.where("_id").is(intentId)
                .and("status").in(PaymentStatus.PENDING, PaymentStatus.FAILED));

        Update u = new Update()
                .set("status", PaymentStatus.PROCESSING)
                .set("idempotencyKey", idempotencyKey)
                .set("method", method)
                .set("updatedAt", Instant.now())
                .inc("attempts", 1);

        PaymentIntent claimed = mongo.findAndModify(q, u,
                FindAndModifyOptions.options().returnNew(true), PaymentIntent.class);

        if (claimed == null) {
            throw new PaymentException("PAYMENT_IN_PROGRESS",
                    "Payment is already being processed");
        }
        return claimed;
    }

    @Transactional
    public PaymentIntent confirm(String intentId, PaymentResult result) {
        Instant now = Instant.now();

        PaymentIntent intent = payments.findById(intentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (intent.getStatus() != PaymentStatus.PROCESSING) {
            throw new PaymentException("PAYMENT_NOT_CONFIRMABLE",
                    "Payment is " + intent.getStatus());
        }

        Booking booking = bookings.findById(intent.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                || !booking.getExpiresAt().isAfter(now)) {
            throw new PaymentException("BOOKING_EXPIRED_DURING_PAYMENT",
                    "The cart expired before payment confirmation");
        }

        Query bookingQuery = Query.query(Criteria.where("_id").is(booking.getId())
                .and("status").is(BookingStatus.PENDING_PAYMENT));

        if (mongo.updateFirst(bookingQuery,
                new Update()
                        .set("status", BookingStatus.CONFIRMED)
                        .set("confirmedAt", now),
                Booking.class).getModifiedCount() != 1) {
            throw new PaymentException("BOOKING_EXPIRED_DURING_PAYMENT",
                    "The booking is no longer pending");
        }

        long updatedSeats = mongo.updateMulti(
                Query.query(Criteria.where("showtimeId").is(booking.getShowtimeId())
                        .and("cartId").is(booking.getCartId())
                        .and("status").is(SeatStatus.HELD)),
                new Update()
                        .set("status", SeatStatus.BOOKED)
                        .set("bookingId", booking.getId())
                        .set("updatedAt", now),
                ShowtimeSeat.class).getModifiedCount();

        if (updatedSeats != booking.getSeats().size()) {
            throw new PaymentException("SEATS_LOST",
                    "Not every seat could be confirmed; the booking was not created");
        }

        long completedCarts = mongo.updateFirst(
                Query.query(Criteria.where("_id").is(booking.getCartId())
                        .and("status").is(CartStatus.CHECKED_OUT)),
                new Update()
                        .set("status", CartStatus.COMPLETED)
                        .set("completedAt", now),
                BookingCart.class).getModifiedCount();

        if (completedCarts != 1) {
            throw new PaymentException("CART_STATE_CHANGED",
                    "The cart is no longer awaiting payment");
        }

        Query intentQuery = Query.query(Criteria.where("_id").is(intentId)
                .and("status").is(PaymentStatus.PROCESSING));

        PaymentIntent paid = mongo.findAndModify(intentQuery,
                new Update()
                        .set("status", PaymentStatus.PAID)
                        .set("reference", result.reference())
                        .set("paidAt", now)
                        .set("updatedAt", now),
                FindAndModifyOptions.options().returnNew(true), PaymentIntent.class);

        if (paid == null) {
            throw new PaymentException("PAYMENT_NOT_CONFIRMABLE",
                    "Payment state changed");
        }

        admission.completeByCart(booking.getCartId());
        return paid;
    }

    @Transactional
    public void fail(String intentId, String reason) {
        mongo.updateFirst(
                Query.query(Criteria.where("_id").is(intentId)
                        .and("status").is(PaymentStatus.PROCESSING)),
                new Update()
                        .set("status", PaymentStatus.FAILED)
                        .set("failureReason", reason)
                        .set("updatedAt", Instant.now()),
                PaymentIntent.class);
    }

    @Transactional
    public void refundRequired(String intentId, String reason) {
        mongo.updateFirst(
                Query.query(Criteria.where("_id").is(intentId)
                        .and("status").is(PaymentStatus.PROCESSING)),
                new Update()
                        .set("status", PaymentStatus.REFUND_REQUIRED)
                        .set("failureReason", reason)
                        .set("updatedAt", Instant.now()),
                PaymentIntent.class);
    }
}
