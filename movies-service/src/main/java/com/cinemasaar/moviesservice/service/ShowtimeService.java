package com.cinemasaar.moviesservice.service;

import com.cinemasaar.moviesservice.model.*;
import com.cinemasaar.moviesservice.repository.*;
import com.cinemasaar.moviesservice.dto.*;
import com.cinemasaar.moviesservice.client.BookingGuard;
import com.cinemasaar.moviesservice.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

// Validates movie screenings and prevents overlapping screen schedules.
@Service @RequiredArgsConstructor
public class ShowtimeService {
    private final ShowtimeRepository showtimes;
    private final MovieRepository movies;
    private final CatalogueService catalogue;
    private final BookingGuard bookingGuard;

    public Showtime get(String id) { return showtimes.findById(id).orElseThrow(() -> ApiException.notFound("Showtime")); }
    public List<Showtime> all() { return showtimes.findAll(); }
    public List<Showtime> byMovie(String id) {
        if (!movies.existsById(id)) throw ApiException.notFound("Movie");
        return showtimes.findByMovieId(id);
    }
    public List<Showtime> byScreen(String id) { catalogue.screen(id); return showtimes.findByScreenId(id); }
    public List<Showtime> byTheater(String id) {
        List<String> ids = catalogue.screens(id).stream().map(Screen::getId).toList();
        return showtimes.findByScreenIdIn(ids);
    }
    public List<Showtime> upcoming() { return showtimes.findByStatusAndStartsAtAfterOrderByStartsAtAsc(ShowtimeStatus.SCHEDULED, Instant.now()); }
    @Transactional
    public Showtime create(ShowtimeRequest request, String creator) {
        if (creator == null || creator.isBlank()) throw ApiException.badRequest("Creator identity is required");
        Showtime showtime = new Showtime(); showtime.setStatus(ShowtimeStatus.SCHEDULED);
        showtime.setCreatedAt(Instant.now()); showtime.setCreatedByUserId(creator);
        apply(showtime, request); return showtimes.save(showtime);
    }
    @Transactional
    public Showtime update(String id, ShowtimeRequest request) {
        Showtime showtime = get(id);
        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) throw ApiException.conflict("Only scheduled showtimes can be edited");
        bookingGuard.requireMutationAllowed(id);
        catalogue.touchScreen(showtime.getScreenId());
        apply(showtime, request); return showtimes.save(showtime);
    }
    private void apply(Showtime showtime, ShowtimeRequest request) {
        if (!request.startsAt().isBefore(request.endsAt())) throw ApiException.badRequest("Start must be before end");
        if (!request.startsAt().isAfter(Instant.now())) throw ApiException.badRequest("Showtime must start in the future");
        if (!movies.existsById(request.movieId())) throw ApiException.notFound("Movie");
        catalogue.touchScreen(request.screenId());
        if (showtimes.overlaps(request.screenId(), request.startsAt(), request.endsAt(), showtime.getId()))
            throw ApiException.conflict("Screen already has an overlapping showtime");
        showtime.setMovieId(request.movieId()); showtime.setScreenId(request.screenId());
        showtime.setStartsAt(request.startsAt()); showtime.setEndsAt(request.endsAt()); showtime.setBasePrice(request.basePrice());
    }
    @Transactional
    public Showtime status(String id, ShowtimeStatus status) {
        Showtime showtime = get(id);
        if (showtime.getStatus() == status) return showtime;
        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED || status == ShowtimeStatus.SCHEDULED)
            throw ApiException.conflict("Terminal showtime status cannot change");
        if (status == ShowtimeStatus.COMPLETED && showtime.getEndsAt().isAfter(Instant.now()))
            throw ApiException.conflict("Showtime has not ended");
        if (status == ShowtimeStatus.CANCELLED) bookingGuard.requireMutationAllowed(id);
        catalogue.touchScreen(showtime.getScreenId()); showtime.setStatus(status);
        return showtimes.save(showtime);
    }
}
