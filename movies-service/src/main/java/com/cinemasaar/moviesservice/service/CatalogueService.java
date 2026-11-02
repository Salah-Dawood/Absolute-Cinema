package com.cinemasaar.moviesservice.service;

import com.cinemasaar.moviesservice.model.*;
import com.cinemasaar.moviesservice.repository.*;
import com.cinemasaar.moviesservice.dto.*;
import com.cinemasaar.moviesservice.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import java.util.*;

// Manages theaters, screens and physical seats only.
@Service @RequiredArgsConstructor
public class CatalogueService {
    private final TheaterRepository theaters;
    private final ScreenRepository screens;
    private final SeatRepository seats;
    private final ShowtimeRepository showtimes;
    private final MongoTemplate mongo;

    public List<Theater> theaters() { return theaters.findAll(); }
    public Theater theater(String id) { return theaters.findById(id).orElseThrow(() -> ApiException.notFound("Theater")); }
    @Transactional
    public Theater saveTheater(String id, TheaterRequest request) {
        Theater theater = id == null ? new Theater() : theater(id);
        theater.setName(request.name().trim()); theater.setLocation(request.location().trim());
        return theaters.save(theater);
    }
    @Transactional
    public void deleteTheater(String id) {
        theater(id);
        touchTheater(id);
        if (screens.existsByTheaterId(id)) throw ApiException.conflict("Remove screens before deleting theater");
        theaters.deleteById(id);
    }
    private void touchTheater(String id) {
        mongo.updateFirst(Query.query(Criteria.where("id").is(id)), new Update().inc("catalogue_version", 1), Theater.class);
    }
    public List<Screen> screens(String theaterId) {
        if (theaterId == null) return screens.findAll();
        theater(theaterId); return screens.findByTheaterId(theaterId);
    }
    public Screen screen(String id) { return screens.findById(id).orElseThrow(() -> ApiException.notFound("Screen")); }
    // Writing the same screen inside each transaction prevents concurrent schedule/layout races.
    public void touchScreen(String id) {
        screen(id);
        mongo.updateFirst(Query.query(Criteria.where("id").is(id)), new Update().inc("scheduleVersion", 1), Screen.class);
    }
    @Transactional
    public Screen saveScreen(String id, ScreenRequest request) {
        theater(request.theaterId()); touchTheater(request.theaterId());
        Screen screen = id == null ? new Screen() : screen(id);
        if (id != null) {
            touchScreen(id);
            if (!screen.getTheaterId().equals(request.theaterId())) {
                touchTheater(screen.getTheaterId());
                if (showtimes.existsByScreenId(id)) throw ApiException.conflict("Scheduled screens cannot move theaters");
            }
        }
        screen.setTheaterId(request.theaterId()); screen.setName(request.name().trim());
        return screens.save(screen);
    }
    @Transactional
    public void deleteScreen(String id) {
        Screen screen = screen(id); touchScreen(id); touchTheater(screen.getTheaterId());
        if (seats.existsByScreenId(id) || showtimes.existsByScreenId(id))
            throw ApiException.conflict("Screen has physical seats or showtimes");
        screens.deleteById(id);
    }
    public List<Seat> seats(String screenId) { screen(screenId); return seats.findByScreenIdOrderByRowLabelAscSeatNumberAsc(screenId); }
    private void editableLayout(String screenId) {
        touchScreen(screenId);
        if (showtimes.existsByScreenId(screenId)) throw ApiException.conflict("Seat layout cannot change after scheduling");
    }
    @Transactional
    public Seat createSeat(SeatRequest request) {
        editableLayout(request.screenId());
        return seats.save(newSeat(request.screenId(), request.rowLabel(), request.seatNumber()));
    }
    private Seat newSeat(String screenId, String row, int number) {
        row = row.toUpperCase(Locale.ROOT);
        if (seats.existsByScreenIdAndRowLabelAndSeatNumber(screenId, row, number))
            throw ApiException.conflict("Seat already exists");
        Seat seat = new Seat(); seat.setScreenId(screenId); seat.setRowLabel(row); seat.setSeatNumber(number);
        return seat;
    }
    @Transactional
    public List<Seat> generateSeats(SeatGenerationRequest request) {
        editableLayout(request.screenId());
        Set<String> rows = new LinkedHashSet<>();
        for (String row : request.rows()) if (!rows.add(row.toUpperCase(Locale.ROOT))) throw ApiException.badRequest("Duplicate row");
        List<Seat> generated = new ArrayList<>();
        for (String row : rows) for (int number = 1; number <= request.seatsPerRow(); number++)
            generated.add(newSeat(request.screenId(), row, number));
        return seats.saveAll(generated);
    }
}
