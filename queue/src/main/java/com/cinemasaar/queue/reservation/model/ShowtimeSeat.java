package com.cinemasaar.queue.reservation.model;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "showtime_seats")
@CompoundIndex(name = "uq_showtime_seat", def = "{'showtimeId': 1, 'seatId': 1}", unique = true)
@CompoundIndex(name = "idx_showtime_status", def = "{'showtimeId': 1, 'status': 1}")
public class ShowtimeSeat {

    @Id
    private String id;
    private String showtimeId;
    private String seatId;
    private String rowLabel;
    private int seatNumber;
    private BigDecimal price;
    private SeatStatus status;
    private String cartId;
    private String heldBy;
    private String bookingId;
    private Instant holdExpiresAt;
    private Instant updatedAt;

    public ShowtimeSeat() {
    }

    public static ShowtimeSeat available(String showtimeId, String seatId, String rowLabel,
                                         int seatNumber, BigDecimal price) {
        ShowtimeSeat seat = new ShowtimeSeat();
        seat.showtimeId = showtimeId;
        seat.seatId = seatId;
        seat.rowLabel = rowLabel;
        seat.seatNumber = seatNumber;
        seat.price = price;
        seat.status = SeatStatus.AVAILABLE;
        seat.updatedAt = Instant.now();
        return seat;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(String showtimeId) {
        this.showtimeId = showtimeId;
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public String getHeldBy() {
        return heldBy;
    }

    public void setHeldBy(String heldBy) {
        this.heldBy = heldBy;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(Instant holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
