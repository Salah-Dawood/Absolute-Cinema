package com.cinemasaar.moviesservice.controller;

import com.cinemasaar.moviesservice.service.CatalogueService;
import com.cinemasaar.moviesservice.dto.TheaterRequest;
import com.cinemasaar.moviesservice.model.Theater;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;

// Exposes theater management endpoints.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/theaters")
public class TheaterController {
    private final CatalogueService service;

    @GetMapping
    public List<Theater> all() {
        return service.theaters();
    }

    @GetMapping("/{id}")
    public Theater get(@PathVariable String id) {
        return service.theater(id);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Theater create(@Valid @RequestBody TheaterRequest request) { return service.saveTheater(null, request); }
    @PutMapping("/{id}") public Theater update(@PathVariable String id, @Valid @RequestBody TheaterRequest request) { return service.saveTheater(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id) { service.deleteTheater(id); }
}
