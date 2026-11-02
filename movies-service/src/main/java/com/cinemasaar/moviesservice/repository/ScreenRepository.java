package com.cinemasaar.moviesservice.repository;

import com.cinemasaar.moviesservice.model.*;
import org.springframework.data.mongodb.repository.*;
import java.util.List;
import java.time.Instant;

// Handles database operations for screen records.
public interface ScreenRepository extends MongoRepository<Screen, String> {
    List<Screen> findByTheaterId(String theaterId);
    boolean existsByTheaterId(String theaterId);
}
