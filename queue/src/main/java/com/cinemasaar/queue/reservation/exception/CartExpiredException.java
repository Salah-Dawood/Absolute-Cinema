package com.cinemasaar.queue.reservation.exception;

public class CartExpiredException extends RuntimeException {

    public CartExpiredException() {
        super("The 15-minute seat hold expired");
    }
}
