package com.cinemasaar.queue.checkout.service;

import com.cinemasaar.queue.checkout.dto.BookingResponse;
import com.cinemasaar.queue.checkout.dto.CheckoutResult;
import com.cinemasaar.queue.checkout.model.Booking;
import com.cinemasaar.queue.checkout.model.BookingStatus;
import com.cinemasaar.queue.checkout.repository.BookingRepository;
import com.cinemasaar.queue.common.exception.NotFoundException;
import com.cinemasaar.queue.payment.model.PaymentIntent;
import com.cinemasaar.queue.payment.repository.PaymentIntentRepository;
import com.cinemasaar.queue.reservation.exception.CartStateException;
import com.cinemasaar.queue.reservation.exception.SeatsLostException;
import com.cinemasaar.queue.reservation.model.BookingCart;
import com.cinemasaar.queue.reservation.model.CartItem;
import com.cinemasaar.queue.reservation.model.SeatStatus;
import com.cinemasaar.queue.reservation.model.ShowtimeSeat;
import com.cinemasaar.queue.reservation.repository.BookingCartRepository;
import com.cinemasaar.queue.reservation.repository.ShowtimeSeatRepository;
import com.cinemasaar.queue.reservation.service.CartService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

    private final CartService cartService;
    private final BookingCartRepository carts;
    private final ShowtimeSeatRepository seats;
    private final BookingRepository bookings;
    private final PaymentIntentRepository payments;

    public CheckoutService(CartService cartService,
                           BookingCartRepository carts,
                           ShowtimeSeatRepository seats,
                           BookingRepository bookings,
                           PaymentIntentRepository payments) {
        this.cartService = cartService;
        this.carts = carts;
        this.seats = seats;
        this.bookings = bookings;
        this.payments = payments;
    }

    @Transactional
    public CheckoutResult checkout(String cartId, String userId) {
        BookingCart cart = cartService.requireOpenCart(cartId, userId);
        if (cart.getItems().isEmpty()) {
            throw new CartStateException("Cart is empty");
        }

        List<ShowtimeSeat> heldSeats = seats.findByShowtimeIdAndCartId(
                cart.getShowtimeId(), cartId);
        Map<String, ShowtimeSeat> bySeat = heldSeats.stream()
                .collect(Collectors.toMap(ShowtimeSeat::getSeatId, Function.identity()));

        List<String> lost = cart.getItems().stream()
                .map(CartItem::getSeatId)
                .filter(seatId -> {
                    ShowtimeSeat seat = bySeat.get(seatId);
                    return seat == null || seat.getStatus() != SeatStatus.HELD;
                })
                .toList();

        if (!lost.isEmpty()) {
            throw new SeatsLostException(lost);
        }

        Instant now = Instant.now();
        String intentId = UUID.randomUUID().toString();
        Booking booking = Booking.pending(UUID.randomUUID().toString(), intentId, cart);
        PaymentIntent intent = PaymentIntent.pending(
                intentId, booking.getId(), booking.getUserId(), booking.getTotal(), now);

        try {
            bookings.insert(booking);
        } catch (DuplicateKeyException concurrentCheckout) {
            // The unique cartId index decides which checkout wins.
            throw new CartStateException("Cart was already checked out");
        }
        payments.insert(intent);

        cart.markCheckedOut(now);
        carts.save(cart);

        return new CheckoutResult(booking, intent);
    }

    public BookingResponse getBooking(String bookingId, String userId) {
        Booking booking = bookings.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        PaymentIntent payment = payments.findByBookingId(bookingId).orElse(null);
        return BookingResponse.from(booking, payment);
    }
}
