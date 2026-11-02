package com.cinemasaar.queue.checkout.scheduler;

import com.cinemasaar.queue.checkout.service.ExpiryService;
import com.cinemasaar.queue.reservation.model.BookingCart;
import com.cinemasaar.queue.reservation.model.CartStatus;
import com.cinemasaar.queue.reservation.repository.BookingCartRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiredCartScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiredCartScheduler.class);

    private final BookingCartRepository carts;
    private final ExpiryService expiryService;

    public ExpiredCartScheduler(BookingCartRepository carts, ExpiryService expiryService) {
        this.carts = carts;
        this.expiryService = expiryService;
    }

    @Scheduled(fixedDelayString = "${app.booking.cart-expiry-interval-ms:2000}")
    public void expireCarts() {
        Instant now = Instant.now();
        List<BookingCart> expired = carts
                .findTop100ByStatusInAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
                        List.of(CartStatus.OPEN, CartStatus.CHECKED_OUT), now);

        for (BookingCart cart : expired) {
            try {
                expiryService.expireCart(cart.getId());
            } catch (Exception e) {
                log.warn("Could not expire cart {}", cart.getId(), e);
            }
        }
    }
}
