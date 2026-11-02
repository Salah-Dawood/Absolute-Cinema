package com.cinemasaar.moviesservice;

import com.cinemasaar.moviesservice.controller.*;
import com.cinemasaar.moviesservice.service.*;
import com.cinemasaar.moviesservice.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HttpValidationTest {
    MovieService service = mock(MovieService.class);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new MovieController(service), new SeatController(mock(CatalogueService.class)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    @Test void invalidSeatReturns400() throws Exception {
        mvc.perform(post("/api/seats").contentType(MediaType.APPLICATION_JSON).content("{\"screenId\":\"s\",\"rowLabel\":\"A\",\"seatNumber\":0}")).andExpect(status().isBadRequest());
    }
    @Test void unknownMovieReturns404() throws Exception {
        when(service.requireMovie("missing")).thenThrow(ApiException.notFound("Movie"));
        mvc.perform(get("/api/movies/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Movie not found"));
    }
    @Test void nullShowingReturns400() throws Exception {
        mvc.perform(patch("/api/movies/id/showing").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }
    @Test void malformedJsonReturns400() throws Exception { mvc.perform(post("/api/seats").contentType(MediaType.APPLICATION_JSON).content("{")).andExpect(status().isBadRequest()); }
    @Test void transactionConflictReturns409WithoutLeakingDetails() throws Exception {
        com.mongodb.MongoException conflict = new com.mongodb.MongoException("internal database details");
        conflict.addLabel("TransientTransactionError");
        when(service.requireMovie("conflict")).thenThrow(new RuntimeException(conflict));
        mvc.perform(get("/api/movies/conflict")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Concurrent catalogue change; reload and retry"));
    }
    @Test void showingUpdateForUnknownMovieReturns404() throws Exception {
        when(service.showing("missing", true)).thenThrow(ApiException.notFound("Movie"));
        mvc.perform(patch("/api/movies/missing/showing").contentType(MediaType.APPLICATION_JSON)
                .content("{\"isShowing\":true}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Movie not found"));
    }
    @Test void unsupportedContentTypeReturns415InsteadOf503() throws Exception {
        mvc.perform(post("/api/seats").contentType(MediaType.MULTIPART_FORM_DATA).content("not JSON"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Content-Type must be application/json"));
    }
}
