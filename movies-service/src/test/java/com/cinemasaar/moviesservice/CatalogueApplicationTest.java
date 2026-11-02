package com.cinemasaar.moviesservice;

import com.cinemasaar.moviesservice.model.Movie;
import com.cinemasaar.moviesservice.repository.MovieRepository;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Loads real Boot configuration and reads an isolated catalogue through the HTTP layer.
@SpringBootTest
class CatalogueApplicationTest {
    static final MongoServer server = new MongoServer(new MemoryBackend());
    static final String uri = server.bindAndGetConnectionString() + "/application_test";
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> uri);
    }
    @Autowired WebApplicationContext context;
    @Autowired MovieRepository movies;
    @Autowired org.springframework.data.mongodb.core.MongoTemplate mongo;
    @BeforeEach void clearMovies() { movies.deleteAll(); }
    @AfterAll static void close() { server.shutdownNow(); }
    @Test void showingEndpointUsesActualRepositoryAndJacksonConfiguration() throws Exception {
        Movie movie = new Movie(); movie.setTitle("Integration movie"); movie.setImdbId("tt1234567"); movie.setIsShowing(true);
        movie.setCreatedAt(java.time.Instant.parse("2026-11-02T10:00:00Z")); movies.save(movie);
        MockMvcBuilders.webAppContextSetup(context).build().perform(get("/api/movies/showing"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Integration movie"))
                .andExpect(jsonPath("$[0].isShowing").value(true)).andExpect(jsonPath("$[0].createdAt").value("2026-11-02T10:00:00Z"));
    }
    @Test void showingUpdateIsPersistedWithoutReplacingMovieMetadata() throws Exception {
        Movie movie = new Movie(); movie.setTitle("Inception"); movie.setImdbId("tt1375666");
        movie.setGenre("Action, Sci-Fi"); movie.setPlot("A preserved movie plot"); movie.setIsShowing(false);
        movie.setCreatedAt(java.time.Instant.parse("2026-11-02T10:00:00Z")); movies.save(movie);
        org.bson.Document filter = new org.bson.Document("imdb_id", "tt1375666");
        org.bson.Document before = mongo.getCollection("movies").find(filter).first();
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();

        mvc.perform(patch("/api/movies/{id}/showing", movie.getId())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"isShowing\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isShowing").value(true))
                .andExpect(jsonPath("$.imdbId").value("tt1375666"));
        mvc.perform(get("/api/movies")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(movie.getId())).andExpect(jsonPath("$[0].isShowing").value(true));
        mvc.perform(get("/api/movies/showing")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(movie.getId()));

        org.bson.Document after = mongo.getCollection("movies").find(filter).first();
        org.assertj.core.api.Assertions.assertThat(after.getBoolean("is_showing")).isTrue();
        org.assertj.core.api.Assertions.assertThat(after.getDate("updated_at")).isNotNull();
        before.remove("is_showing"); before.remove("updated_at");
        after.remove("is_showing"); after.remove("updated_at");
        org.assertj.core.api.Assertions.assertThat(after).isEqualTo(before);
    }
}
