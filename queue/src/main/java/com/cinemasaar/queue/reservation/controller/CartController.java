package com.cinemasaar.queue.reservation.controller;

import com.cinemasaar.queue.common.TransientTransactionRetrier;
import com.cinemasaar.queue.config.BookingProperties;
import com.cinemasaar.queue.reservation.dto.AddonResponse;
import com.cinemasaar.queue.reservation.dto.AddonSelectionRequest;
import com.cinemasaar.queue.reservation.dto.CartResponse;
import com.cinemasaar.queue.reservation.dto.CreateCartRequest;
import com.cinemasaar.queue.reservation.service.CartService;
import com.cinemasaar.queue.reservation.service.SeatOperationExecutor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CartController {

    private final CartService cartService;
    private final SeatOperationExecutor executor;
    private final TransientTransactionRetrier retrier;

    public CartController(CartService cartService,
                          SeatOperationExecutor executor,
                          TransientTransactionRetrier retrier) {
        this.cartService = cartService;
        this.executor = executor;
        this.retrier = retrier;
    }

    @PostMapping("/carts")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse create(@RequestHeader("X-User-Id") String userId,
                               @Valid @RequestBody CreateCartRequest request) {
        return CartResponse.from(retrier.execute(
                () -> cartService.createCart(userId, request.admissionToken())));
    }

    @PostMapping("/carts/{cartId}/seats/{seatId}")
    public CartResponse addSeat(@PathVariable String cartId,
                                @PathVariable String seatId,
                                @RequestHeader("X-User-Id") String userId) {
        return CartResponse.from(
                executor.submit(() -> cartService.addSeat(cartId, userId, seatId)));
    }

    @DeleteMapping("/carts/{cartId}/seats/{seatId}")
    public CartResponse removeSeat(@PathVariable String cartId,
                                   @PathVariable String seatId,
                                   @RequestHeader("X-User-Id") String userId) {
        return CartResponse.from(cartService.removeSeat(cartId, userId, seatId));
    }

    @PutMapping("/carts/{cartId}/seats/{seatId}/addons")
    public CartResponse addons(@PathVariable String cartId,
                               @PathVariable String seatId,
                               @RequestHeader("X-User-Id") String userId,
                               @Valid @RequestBody AddonSelectionRequest request) {
        return CartResponse.from(
                cartService.setAddons(cartId, userId, seatId, request.items()));
    }

    @GetMapping("/carts/{cartId}")
    public CartResponse get(@PathVariable String cartId,
                            @RequestHeader("X-User-Id") String userId) {
        return CartResponse.from(cartService.getCart(cartId, userId));
    }
}
