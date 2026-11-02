package com.cinemasaar.moviesservice.controller;

import com.cinemasaar.moviesservice.service.MovieService;
import com.cinemasaar.moviesservice.dto.*;
import com.cinemasaar.moviesservice.model.Movie;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;

// Exposes local catalogue queries and explicit OMDb import operations.
@RestController @RequiredArgsConstructor @RequestMapping("/api/movies")
public class MovieController {
    private final MovieService service;
    @GetMapping public List<Movie> all() { return service.getAllMovies(); }
    @GetMapping("/showing") public List<Movie> showing() { return service.getShowingMovies(); }
    @GetMapping("/{id}") public Movie get(@PathVariable String id) { return service.requireMovie(id); }
    @GetMapping("/imdb/{imdbId}") public Movie imdb(@PathVariable String imdbId) { return service.byImdb(imdbId); }
    @GetMapping("/search") public List<Movie> search(@RequestParam(required=false) String title,
            @RequestParam(required=false) String genre, @RequestParam(required=false) String language,
            @RequestParam(required=false) String year, @RequestParam(required=false) String type,
            @RequestParam(required=false) Boolean isShowing) { return service.search(title, genre, language, year, type, isShowing); }
    @GetMapping("/omdb/search") public OmdbSearchResponse omdb(@RequestParam String title, @RequestParam(defaultValue="1") int page) { return service.searchOmdb(title, page); }
    @PostMapping("/import/{imdbId}") @ResponseStatus(HttpStatus.CREATED) public Movie importMovie(@PathVariable String imdbId) { return service.importMovie(imdbId); }
    @PostMapping("/{id}/refresh") public Movie refresh(@PathVariable String id) { return service.refresh(id); }
    @PatchMapping("/{id}/showing") public Movie showing(@PathVariable String id, @Valid @RequestBody ShowingRequest request) { return service.showing(id, request.isShowing()); }
    @PutMapping("/{id}") public Movie edit(@PathVariable String id, @Valid @RequestBody MovieUpdateRequest request) { return service.edit(id, request); }
}
