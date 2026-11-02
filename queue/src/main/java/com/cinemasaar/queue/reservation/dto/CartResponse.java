package com.cinemasaar.queue.reservation.dto;

import com.cinemasaar.queue.reservation.model.AddonLine;
import com.cinemasaar.queue.reservation.model.BookingCart;
import com.cinemasaar.queue.reservation.model.CartItem;
import com.cinemasaar.queue.reservation.model.CartStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record CartResponse(
        String cartId,
        String showtimeId,
        CartStatus status,
        Instant expiresAt,
        long secondsRemaining,
        List<Item> items,
        BigDecimal seatTotal,
        BigDecimal addonTotal,
        BigDecimal total
) {

    public static CartResponse from(BookingCart cart) {
        Instant now = Instant.now();
        long seconds = cart.getExpiresAt() == null
                ? 0
                : Math.max(0, Duration.between(now, cart.getExpiresAt()).getSeconds());

        return new CartResponse(
                cart.getId(),
                cart.getShowtimeId(),
                cart.getStatus(),
                cart.getExpiresAt(),
                seconds,
                cart.getItems().stream().map(Item::from).toList(),
                cart.getSeatTotal(),
                cart.getAddonTotal(),
                cart.getTotal());
    }

    public record Item(
            String seatId,
            String rowLabel,
            int seatNumber,
            BigDecimal seatPrice,
            List<AddonLine> addons,
            BigDecimal lineTotal
    ) {
        public static Item from(CartItem item) {
            return new Item(
                    item.getSeatId(),
                    item.getRowLabel(),
                    item.getSeatNumber(),
                    item.getSeatPrice(),
                    item.getAddons(),
                    item.getSeatPrice().add(item.addonTotal()));
        }
    }
}
