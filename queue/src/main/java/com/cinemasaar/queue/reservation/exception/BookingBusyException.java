package com.cinemasaar.queue.reservation.exception;

public class BookingBusyException extends RuntimeException {

    public BookingBusyException(String message) {
        super(message);
    }

    public BookingBusyException(String message, Throwable cause) {
        super(message, cause);
    }
}
