package com.cinemasaar.moviesservice.repository;

import com.cinemasaar.moviesservice.model.Movie;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

// This interface handles database operations for Movie objects
// It connects our Spring Boot movie-service to the "movies" collection in MongoDB Atlas.

public interface MovieRepository extends MongoRepository<Movie, String> {
    // Find a movie using its IMDb ID (provided by OMDb)
    Optional<Movie> findByImdbId(String imdbId);

    // Retrieve all movies currently showing in our cinema
    List<Movie> findByIsShowingTrue();
}