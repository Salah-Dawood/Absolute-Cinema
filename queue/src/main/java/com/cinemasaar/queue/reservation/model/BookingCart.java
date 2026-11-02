package com.cinemasaar.queue.reservation.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "booking_carts")
@CompoundIndex(name = "idx_status_expires", def = "{'status': 1, 'expiresAt': 1}")
public class BookingCart {

    @Id
    private String id;
    private String queueEntryId;
    private String userId;
    private String showtimeId;
    private CartStatus status;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant checkedOutAt;
    private Instant completedAt;
    private Instant expiredAt;
    private List<CartItem> items = new ArrayList<>();
    private BigDecimal seatTotal = BigDecimal.ZERO;
    private BigDecimal addonTotal = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;

    @Version
    private Long version;

    public BookingCart() {
    }

    public static BookingCart open(String id, String queueEntryId, String userId,
                                   String showtimeId, Instant now, Instant expiresAt) {
        BookingCart cart = new BookingCart();
        cart.id = id;
        cart.queueEntryId = queueEntryId;
        cart.userId = userId;
        cart.showtimeId = showtimeId;
        cart.status = CartStatus.OPEN;
        cart.createdAt = now;
        cart.expiresAt = expiresAt;
        return cart;
    }

    public boolean hasSeat(String seatId) {
        return items.stream().anyMatch(i -> i.getSeatId().equals(seatId));
    }

    public Optional<CartItem> item(String seatId) {
        return items.stream().filter(i -> i.getSeatId().equals(seatId)).findFirst();
    }

    public void addItem(ShowtimeSeat seat) {
        items.add(CartItem.from(seat));
        recalculate();
    }

    public CartItem removeItem(String seatId) {
        CartItem item = item(seatId).orElse(null);
        if (item != null) {
            items.remove(item);
            recalculate();
        }
        return item;
    }

    public void recalculate() {
        seatTotal = items.stream()
                .map(CartItem::getSeatPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        addonTotal = items.stream()
                .map(CartItem::addonTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        total = seatTotal.add(addonTotal);
    }

    public void markCheckedOut(Instant now) {
        status = CartStatus.CHECKED_OUT;
        checkedOutAt = now;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQueueEntryId() {
        return queueEntryId;
    }

    public void setQueueEntryId(String queueEntryId) {
        this.queueEntryId = queueEntryId;
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

    public CartStatus getStatus() {
        return status;
    }

    public void setStatus(CartStatus status) {
        this.status = status;
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

    public Instant getCheckedOutAt() {
        return checkedOutAt;
    }

    public void setCheckedOutAt(Instant checkedOutAt) {
        this.checkedOutAt = checkedOutAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
