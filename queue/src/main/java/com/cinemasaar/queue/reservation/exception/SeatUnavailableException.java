package com.cinemasaar.queue.reservation.exception;

public class SeatUnavailableException extends RuntimeException {

    private final String seatId;

    public SeatUnavailableException(String seatId) {
        super("Seat is already held or booked: " + seatId);
        this.seatId = seatId;
    }

    public String getSeatId() {
        return seatId;
    }
}
