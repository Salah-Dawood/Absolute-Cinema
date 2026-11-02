package com.cinemasaar.moviesservice;

import com.cinemasaar.moviesservice.dto.*;
import com.cinemasaar.moviesservice.exception.*;
import com.cinemasaar.moviesservice.model.*;
import com.cinemasaar.moviesservice.repository.*;
import com.cinemasaar.moviesservice.service.*;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CatalogueServiceTest {
    TheaterRepository theaters = mock(TheaterRepository.class); ScreenRepository screens = mock(ScreenRepository.class);
    SeatRepository seats = mock(SeatRepository.class); ShowtimeRepository showtimes = mock(ShowtimeRepository.class);
    CatalogueService service = new CatalogueService(theaters, screens, seats, showtimes, mock(MongoTemplate.class));
    @BeforeEach void setup() { Screen screen = new Screen(); screen.setId("s"); screen.setTheaterId("t"); when(screens.findById("s")).thenReturn(Optional.of(screen)); }
    @Test void rejectsMissingTheater() {
        assertThatThrownBy(() -> service.saveScreen(null, new ScreenRequest("missing", "Screen 1"))).isInstanceOf(ApiException.class).hasMessage("Theater not found");
        verify(screens, never()).save(any());
    }
    @Test void rejectsMissingScreen() { assertThatThrownBy(() -> service.createSeat(new SeatRequest("missing", "A", 1))).hasMessage("Screen not found"); }
    @Test void rejectsDuplicateSeatAfterNormalizingRow() {
        when(seats.existsByScreenIdAndRowLabelAndSeatNumber("s", "A", 1)).thenReturn(true);
        assertThatThrownBy(() -> service.createSeat(new SeatRequest("s", "a", 1))).hasMessage("Seat already exists"); verify(seats, never()).save(any());
    }
    @Test void scheduledScreenLayoutCannotChange() {
        when(showtimes.existsByScreenId("s")).thenReturn(true);
        assertThatThrownBy(() -> service.createSeat(new SeatRequest("s", "A", 1))).hasMessageContaining("cannot change");
    }
    @Test void generationRejectsDuplicateRowsBeforeSaving() {
        assertThatThrownBy(() -> service.generateSeats(new SeatGenerationRequest("s", List.of("A", "a"), 10))).hasMessage("Duplicate row");
        verify(seats, never()).saveAll(any());
    }
    @Test void screenWithSeatsCannotBeDeleted() {
        when(seats.existsByScreenId("s")).thenReturn(true);
        assertThatThrownBy(() -> service.deleteScreen("s")).hasMessageContaining("physical seats"); verify(screens, never()).deleteById(any());
    }
}
