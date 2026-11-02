package com.cinemasaar.moviesservice.repository;

import com.cinemasaar.moviesservice.model.*;
import org.springframework.data.mongodb.repository.*;
import java.util.List;
import java.time.Instant;

// Handles database operations for showtime records.
public interface ShowtimeRepository extends MongoRepository<Showtime, String> {
    List<Showtime> findByMovieId(String movieId);
    List<Showtime> findByScreenId(String screenId);
    List<Showtime> findByScreenIdIn(List<String> screenIds);
    List<Showtime> findByStatusAndStartsAtAfterOrderByStartsAtAsc(ShowtimeStatus status, Instant now);
    boolean existsByScreenId(String screenId);
    @Query(value = "{ 'screen_id': ?0, 'status': { $ne: 'CANCELLED' }, 'starts_at': { $lt: ?2 }, 'ends_at': { $gt: ?1 }, '_id': { $ne: ?3 } }", exists = true)
    boolean overlaps(String screenId, Instant startsAt, Instant endsAt, String excludedId);
}
