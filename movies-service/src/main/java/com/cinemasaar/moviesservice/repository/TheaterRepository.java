package com.cinemasaar.moviesservice.repository;

import com.cinemasaar.moviesservice.model.*;
import org.springframework.data.mongodb.repository.*;
import java.util.List;
import java.time.Instant;

// Handles database operations for theater records.
public interface TheaterRepository extends MongoRepository<Theater, String> {
    
}
