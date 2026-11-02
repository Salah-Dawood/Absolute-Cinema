package com.cinemasaar.queue.reservation.controller;

import com.cinemasaar.queue.reservation.dto.SeatMapResponse;
import com.cinemasaar.queue.reservation.dto.SeedSeatsRequest;
import com.cinemasaar.queue.reservation.model.ShowtimeSeat;
import com.cinemasaar.queue.reservation.repository.ShowtimeSeatRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SeatCatalogController {

    private final ShowtimeSeatRepository seats;

    public SeatCatalogController(ShowtimeSeatRepository seats) {
        this.seats = seats;
    }

    @GetMapping("/showtimes/{showtimeId}/seats")
    public SeatMapResponse seats(@PathVariable String showtimeId) {
        return SeatMapResponse.from(
                showtimeId,
                seats.findByShowtimeIdOrderByRowLabelAscSeatNumberAsc(showtimeId),
                Instant.now());
    }

    @PostMapping("/admin/showtimes/{showtimeId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public SeatMapResponse seed(@PathVariable String showtimeId,
                                @Valid @RequestBody SeedSeatsRequest request) {
        if (seats.countByShowtimeId(showtimeId) > 0) {
            return seats(showtimeId);
        }

        List<ShowtimeSeat> generated = new ArrayList<>();
        for (String row : request.rows()) {
            for (int number = 1; number <= request.seatsPerRow(); number++) {
                generated.add(ShowtimeSeat.available(
                        showtimeId,
                        row + number,
                        row,
                        number,
                        request.price()));
            }
        }

        try {
            seats.insert(generated);
        } catch (DuplicateKeyException concurrentSeed) {
            // another request seeded this showtime first; the unique index decided the winner
        }
        return seats(showtimeId);
    }
}
