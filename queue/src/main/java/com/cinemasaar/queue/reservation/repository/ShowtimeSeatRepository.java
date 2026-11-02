package com.cinemasaar.queue.reservation.repository;

import com.cinemasaar.queue.reservation.model.ShowtimeSeat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShowtimeSeatRepository extends MongoRepository<ShowtimeSeat, String> {

    Optional<ShowtimeSeat> findByShowtimeIdAndSeatId(String showtimeId, String seatId);

    List<ShowtimeSeat> findByShowtimeIdOrderByRowLabelAscSeatNumberAsc(String showtimeId);

    List<ShowtimeSeat> findByShowtimeIdAndCartId(String showtimeId, String cartId);

    long countByShowtimeId(String showtimeId);
}
