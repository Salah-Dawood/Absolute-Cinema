package com.cinemasaar.queue.checkout.controller;

import com.cinemasaar.queue.checkout.dto.BookingResponse;
import com.cinemasaar.queue.checkout.dto.CheckoutResult;
import com.cinemasaar.queue.checkout.service.CheckoutService;
import com.cinemasaar.queue.reservation.service.SeatOperationExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final SeatOperationExecutor executor;

    public CheckoutController(CheckoutService checkoutService, SeatOperationExecutor executor) {
        this.checkoutService = checkoutService;
        this.executor = executor;
    }

    @PostMapping("/carts/{cartId}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse checkout(@PathVariable String cartId,
                                    @RequestHeader("X-User-Id") String userId) {
        CheckoutResult result = executor.submit(
                () -> checkoutService.checkout(cartId, userId));
        return BookingResponse.from(result.booking(), result.intent());
    }

    @GetMapping("/bookings/{bookingId}")
    public BookingResponse booking(@PathVariable String bookingId,
                                   @RequestHeader("X-User-Id") String userId) {
        return checkoutService.getBooking(bookingId, userId);
    }
}
