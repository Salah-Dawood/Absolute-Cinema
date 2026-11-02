package com.cinemasaar.moviesservice;

import com.cinemasaar.moviesservice.model.*;
import com.cinemasaar.moviesservice.repository.*;
import com.mongodb.client.*;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import java.time.Instant;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

// Exercises real Spring repositories against an isolated Mongo wire-protocol fake.
// This verifies mapping, indexes and queries; replica-set transaction races need Atlas/local replica-set testing.
class RepositoryTest {
    MongoServer server;
    MongoClient client;
    MongoTemplate mongo;
    MovieRepository movies;
    ShowtimeRepository showtimes;
    @BeforeEach void setup() {
        server = new MongoServer(new MemoryBackend());
        client = MongoClients.create(server.bindAndGetConnectionString());
        mongo = new MongoTemplate(client, "catalogue_test");
        MongoRepositoryFactory factory = new MongoRepositoryFactory(mongo);
        movies = factory.getRepository(MovieRepository.class); showtimes = factory.getRepository(ShowtimeRepository.class);
        mongo.indexOps(Movie.class).createIndex(new Index().on("imdb_id", Sort.Direction.ASC).unique().sparse());
    }
    @AfterEach void close() { if (client != null) client.close(); if (server != null) server.shutdownNow(); }
    @Test void movieLookupAndShowingUseMappedFields() {
        Movie movie = new Movie(); movie.setImdbId("tt0133093"); movie.setTitle("The Matrix"); movie.setIsShowing(true);
        movie.setImdbRating(new BigDecimal("8.7")); movie.setCreatedAt(Instant.parse("2026-11-02T00:00:00Z"));
        movies.save(movie);
        assertThat(movies.findByImdbId("tt0133093")).isPresent(); assertThat(movies.findByIsShowingTrue()).hasSize(1);
        org.bson.Document stored = mongo.getCollection("movies").find().first();
        assertThat(stored).containsKeys("imdb_id", "is_showing", "created_at").doesNotContainKeys("imdbId", "isShowing");
        assertThat(stored.get("imdb_rating")).isInstanceOf(org.bson.types.Decimal128.class);
    }
    @Test void databaseRejectsConcurrentStyleDuplicateImports() {
        Movie first = new Movie(); first.setImdbId("tt0133093"); movies.save(first);
        Movie duplicate = new Movie(); duplicate.setImdbId("tt0133093");
        assertThatThrownBy(() -> movies.save(duplicate)).isInstanceOf(DuplicateKeyException.class);
    }
    @Test void overlapQueryAllowsAdjacentButRejectsIntersectingIntervals() {
        Instant start = Instant.parse("2027-01-01T10:00:00Z"), end = start.plusSeconds(7200);
        Showtime showtime = new Showtime(); showtime.setScreenId("screen"); showtime.setStartsAt(start); showtime.setEndsAt(end);
        showtime.setStatus(ShowtimeStatus.SCHEDULED); showtime.setBasePrice(new BigDecimal("4.50")); showtimes.save(showtime);
        assertThat(showtimes.overlaps("screen", start.plusSeconds(60), end.plusSeconds(60), null)).isTrue();
        assertThat(showtimes.overlaps("screen", end, end.plusSeconds(7200), null)).isFalse();
        assertThat(showtimes.overlaps("screen", start.minusSeconds(7200), start, null)).isFalse();
        assertThat(showtimes.overlaps("screen", start, end, showtime.getId())).isFalse();
        assertThat(showtimes.overlaps("other", start, end, null)).isFalse();
        showtime.setStatus(ShowtimeStatus.CANCELLED); showtimes.save(showtime);
        assertThat(showtimes.overlaps("screen", start, end, null)).isFalse();
    }
    @Test void seatIndexRejectsDuplicateLocationWithinOneScreen() {
        mongo.indexOps(Seat.class).createIndex(new CompoundIndexDefinition(new org.bson.Document("screen_id",1).append("row_label",1).append("seat_number",1)).unique());
        SeatRepository seats = new MongoRepositoryFactory(mongo).getRepository(SeatRepository.class);
        Seat first = new Seat(); first.setScreenId("s"); first.setRowLabel("A"); first.setSeatNumber(1); seats.save(first);
        Seat duplicate = new Seat(); duplicate.setScreenId("s"); duplicate.setRowLabel("A"); duplicate.setSeatNumber(1);
        assertThatThrownBy(() -> seats.save(duplicate)).isInstanceOf(DuplicateKeyException.class);
        duplicate.setScreenId("other"); seats.save(duplicate);
        assertThat(seats.findByScreenIdOrderByRowLabelAscSeatNumberAsc("s")).hasSize(1);
    }
}
