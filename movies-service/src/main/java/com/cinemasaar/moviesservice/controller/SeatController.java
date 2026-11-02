package com.cinemasaar.moviesservice.controller;

import com.cinemasaar.moviesservice.service.CatalogueService;
import com.cinemasaar.moviesservice.dto.*;
import com.cinemasaar.moviesservice.model.Seat;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;

// Defines physical seats; availability belongs to Member 3.
@RestController @RequiredArgsConstructor @RequestMapping("/api/seats")
public class SeatController {
    private final CatalogueService service;
    @GetMapping("/screen/{screenId}") public List<Seat> byScreen(@PathVariable String screenId) { return service.seats(screenId); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Seat create(@Valid @RequestBody SeatRequest request) { return service.createSeat(request); }
    @PostMapping("/generate") @ResponseStatus(HttpStatus.CREATED) public List<Seat> generate(@Valid @RequestBody SeatGenerationRequest request) { return service.generateSeats(request); }
}
