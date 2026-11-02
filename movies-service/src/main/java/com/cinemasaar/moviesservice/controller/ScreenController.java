package com.cinemasaar.moviesservice.controller;

import com.cinemasaar.moviesservice.service.CatalogueService;
import com.cinemasaar.moviesservice.dto.ScreenRequest;
import com.cinemasaar.moviesservice.model.Screen;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;

// Exposes screens belonging to theaters.
@RestController @RequiredArgsConstructor @RequestMapping("/api/screens")
public class ScreenController {
    private final CatalogueService service;
    @GetMapping public List<Screen> all() { return service.screens(null); }
    @GetMapping("/theater/{theaterId}") public List<Screen> byTheater(@PathVariable String theaterId) { return service.screens(theaterId); }
    @GetMapping("/{id}") public Screen get(@PathVariable String id) { return service.screen(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Screen create(@Valid @RequestBody ScreenRequest request) { return service.saveScreen(null, request); }
    @PutMapping("/{id}") public Screen update(@PathVariable String id, @Valid @RequestBody ScreenRequest request) { return service.saveScreen(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id) { service.deleteScreen(id); }
}
