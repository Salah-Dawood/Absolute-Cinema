package com.cinemasaar.queue.checkout.model;

import com.cinemasaar.queue.reservation.model.BookingCart;
import com.cinemasaar.queue.reservation.model.CartItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "bookings")
@CompoundIndex(name = "uq_cart", def = "{'cartId': 1}", unique = true)
public class Booking {

    @Id
    private String id;
    private String cartId;
    private String userId;
    private String showtimeId;
    private String paymentIntentId;
    private BookingStatus status;
    private List<CartItem> seats;
    private BigDecimal seatTotal;
    private BigDecimal addonTotal;
    private BigDecimal total;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant confirmedAt;
    private Instant expiredAt;

    public Booking() {
    }

    public static Booking pending(String id, String paymentIntentId, BookingCart cart) {
        Booking booking = new Booking();
        booking.id = id;
        booking.cartId = cart.getId();
        booking.userId = cart.getUserId();
        booking.showtimeId = cart.getShowtimeId();
        booking.paymentIntentId = paymentIntentId;
        booking.status = BookingStatus.PENDING_PAYMENT;
        booking.seats = new ArrayList<>(cart.getItems());
        booking.seatTotal = cart.getSeatTotal();
        booking.addonTotal = cart.getAddonTotal();
        booking.total = cart.getTotal();
        booking.createdAt = Instant.now();
        booking.expiresAt = cart.getExpiresAt();
        return booking;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(String showtimeId) {
        this.showtimeId = showtimeId;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public List<CartItem> getSeats() {
        return seats;
    }

    public void setSeats(List<CartItem> seats) {
        this.seats = seats;
    }

    public BigDecimal getSeatTotal() {
        return seatTotal;
    }

    public void setSeatTotal(BigDecimal seatTotal) {
        this.seatTotal = seatTotal;
    }

    public BigDecimal getAddonTotal() {
        return addonTotal;
    }

    public void setAddonTotal(BigDecimal addonTotal) {
        this.addonTotal = addonTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }
}
