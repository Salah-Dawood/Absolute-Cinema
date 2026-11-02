package com.cinemasaar.moviesservice.repository;

import com.cinemasaar.moviesservice.model.*;
import org.springframework.data.mongodb.repository.*;
import java.util.List;
import java.time.Instant;

// Handles database operations for seat records.
public interface SeatRepository extends MongoRepository<Seat, String> {
    List<Seat> findByScreenIdOrderByRowLabelAscSeatNumberAsc(String screenId);
    boolean existsByScreenIdAndRowLabelAndSeatNumber(String screenId, String rowLabel, Integer seatNumber);
    boolean existsByScreenId(String screenId);
}
