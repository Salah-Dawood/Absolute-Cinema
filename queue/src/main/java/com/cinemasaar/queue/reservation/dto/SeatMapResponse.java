package com.cinemasaar.queue.reservation.dto;

import com.cinemasaar.queue.reservation.model.SeatStatus;
import com.cinemasaar.queue.reservation.model.ShowtimeSeat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record SeatMapResponse(String showtimeId, List<Row> rows) {

    public static SeatMapResponse from(String showtimeId, List<ShowtimeSeat> seats, Instant now) {
        Map<String, List<ShowtimeSeat>> byRow = seats.stream()
                .collect(Collectors.groupingBy(
                        ShowtimeSeat::getRowLabel,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<Row> rows = byRow.entrySet().stream()
                .map(entry -> new Row(
                        entry.getKey(),
                        entry.getValue().stream().map(seat -> toSeat(seat, now)).toList()))
                .toList();

        return new SeatMapResponse(showtimeId, rows);
    }

    private static Seat toSeat(ShowtimeSeat seat, Instant now) {
        SeatStatus effective = seat.getStatus();
        if (effective == SeatStatus.HELD
                && seat.getHoldExpiresAt() != null
                && !seat.getHoldExpiresAt().isAfter(now)) {
            effective = SeatStatus.AVAILABLE;
        }
        return new Seat(
                seat.getSeatId(),
                seat.getSeatNumber(),
                effective,
                seat.getPrice(),
                seat.getHoldExpiresAt());
    }

    public record Row(String label, List<Seat> seats) {
    }

    public record Seat(
            String seatId,
            int number,
            SeatStatus status,
            BigDecimal price,
            Instant holdExpiresAt
    ) {
    }
}
