package com.cinemasaar.moviesservice;

import com.cinemasaar.moviesservice.client.*;
import com.cinemasaar.moviesservice.dto.*;
import com.cinemasaar.moviesservice.model.*;
import com.cinemasaar.moviesservice.repository.*;
import com.cinemasaar.moviesservice.service.*;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ShowtimeServiceTest {
    ShowtimeRepository repository = mock(ShowtimeRepository.class); MovieRepository movies = mock(MovieRepository.class);
    CatalogueService catalogue = mock(CatalogueService.class); BookingGuard guard = mock(BookingGuard.class);
    ShowtimeService service = new ShowtimeService(repository, movies, catalogue, guard);
    Instant start = Instant.now().plusSeconds(3600), end = start.plusSeconds(7200);
    ShowtimeRequest request = new ShowtimeRequest("m", "s", start, end, new BigDecimal("4.50"));
    @BeforeEach void setup() { when(movies.existsById("m")).thenReturn(true); when(repository.save(any())).thenAnswer(i -> i.getArgument(0)); }
    @Test void rejectsOverlap() {
        when(repository.overlaps("s", start, end, null)).thenReturn(true);
        assertThatThrownBy(() -> service.create(request, "u")).hasMessageContaining("overlapping"); verify(repository, never()).save(any());
    }
    @Test void rejectsReversedOrEqualTimes() {
        for (Instant invalid : new Instant[]{start, start.minusSeconds(1)})
            assertThatThrownBy(() -> service.create(new ShowtimeRequest("m", "s", start, invalid, BigDecimal.ONE), "u")).hasMessage("Start must be before end");
    }
    @Test void requiresMovieAndScreen() {
        when(movies.existsById("m")).thenReturn(false);
        assertThatThrownBy(() -> service.create(request, "u")).hasMessage("Movie not found");
        when(movies.existsById("m")).thenReturn(true);
        doThrow(com.cinemasaar.moviesservice.exception.ApiException.notFound("Screen")).when(catalogue).touchScreen("s");
        assertThatThrownBy(() -> service.create(request, "u")).hasMessage("Screen not found");
    }
    @Test void createsScheduledShowtimeWithCreatorAndTime() {
        Showtime result = service.create(request, "u"); assertThat(result.getStatus()).isEqualTo(ShowtimeStatus.SCHEDULED);
        assertThat(result.getCreatedByUserId()).isEqualTo("u"); assertThat(result.getBasePrice()).isEqualByComparingTo("4.50");
        verify(catalogue).touchScreen("s");
    }
    @Test void mutationRequiresBookingPermission() {
        Showtime existing = new Showtime(); existing.setStatus(ShowtimeStatus.SCHEDULED); existing.setScreenId("s");
        when(repository.findById("id")).thenReturn(Optional.of(existing));
        doThrow(com.cinemasaar.moviesservice.exception.ApiException.conflict("Booked")).when(guard).requireMutationAllowed("id");
        assertThatThrownBy(() -> service.update("id", request)).hasMessage("Booked");
        assertThatThrownBy(() -> service.status("id", ShowtimeStatus.CANCELLED)).hasMessage("Booked");
    }
    @Test void completedShowtimeCannotReopen() {
        Showtime existing = new Showtime(); existing.setStatus(ShowtimeStatus.COMPLETED);
        when(repository.findById("id")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.status("id", ShowtimeStatus.SCHEDULED)).hasMessageContaining("Terminal");
    }
}
