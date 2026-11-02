package com.cinemasaar.moviesservice.controller;

import com.cinemasaar.moviesservice.service.ShowtimeService;
import com.cinemasaar.moviesservice.dto.*;
import com.cinemasaar.moviesservice.model.Showtime;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;

// Supplies screening information to booking and concurrency services.
@RestController @RequiredArgsConstructor @RequestMapping("/api/showtimes")
public class ShowtimeController {
    private final ShowtimeService service;
    @GetMapping public List<Showtime> all() { return service.all(); }
    @GetMapping("/{id}") public Showtime get(@PathVariable String id) { return service.get(id); }
    @GetMapping("/movie/{movieId}") public List<Showtime> byMovie(@PathVariable String movieId) { return service.byMovie(movieId); }
    @GetMapping("/screen/{screenId}") public List<Showtime> byScreen(@PathVariable String screenId) { return service.byScreen(screenId); }
    @GetMapping("/theater/{theaterId}") public List<Showtime> byTheater(@PathVariable String theaterId) { return service.byTheater(theaterId); }
    @GetMapping("/upcoming") public List<Showtime> upcoming() { return service.upcoming(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Showtime create(@Valid @RequestBody ShowtimeRequest request, @RequestHeader("X-User-Id") String creator) { return service.create(request, creator); }
    @PutMapping("/{id}") public Showtime update(@PathVariable String id, @Valid @RequestBody ShowtimeRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/status") public Showtime status(@PathVariable String id, @Valid @RequestBody StatusRequest request) { return service.status(id, request.status()); }
}
