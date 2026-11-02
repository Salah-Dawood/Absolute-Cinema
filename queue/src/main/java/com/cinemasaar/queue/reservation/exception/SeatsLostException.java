package com.cinemasaar.queue.reservation.exception;

import java.util.List;

public class SeatsLostException extends RuntimeException {

    private final List<String> seatIds;

    public SeatsLostException(List<String> seatIds) {
        super("Some seats are no longer held: " + String.join(", ", seatIds));
        this.seatIds = List.copyOf(seatIds);
    }

    public List<String> getSeatIds() {
        return seatIds;
    }
}
