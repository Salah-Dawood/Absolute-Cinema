package com.cinemasaar.moviesservice;

import com.cinemasaar.moviesservice.client.*;
import com.cinemasaar.moviesservice.dto.*;
import com.cinemasaar.moviesservice.exception.*;
import com.cinemasaar.moviesservice.model.*;
import com.cinemasaar.moviesservice.repository.*;
import com.cinemasaar.moviesservice.service.*;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.mockito.ArgumentCaptor;
import java.util.*;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class MovieServiceTest {
    MovieRepository repository = mock(MovieRepository.class);
    MongoTemplate mongo = mock(MongoTemplate.class);
    OmdbClient omdb = mock(OmdbClient.class);
    MovieService service = new MovieService(repository, mongo, omdb, new OmdbMapper());
    @Test void duplicateImportDoesNotCallProvider() {
        when(repository.findByImdbId("tt0133093")).thenReturn(Optional.of(new Movie()));
        assertThatThrownBy(() -> service.importMovie("tt0133093")).isInstanceOf(ApiException.class).hasMessageContaining("already imported");
        verifyNoInteractions(omdb);
    }
    @Test void importsMetadataWithLocalDefaults() {
        OmdbMovieResponse response = new OmdbMovieResponse(); response.setImdbId("tt0133093"); response.setTitle("The Matrix");
        when(repository.findByImdbId("tt0133093")).thenReturn(Optional.empty()); when(omdb.movie("tt0133093")).thenReturn(response);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        Movie movie = service.importMovie("tt0133093");
        assertThat(movie.getIsShowing()).isFalse(); assertThat(movie.getCreatedAt()).isNotNull(); assertThat(movie.getOmdbLastSyncedAt()).isNotNull();
    }
    @Test void mapsMissingAndNumericProviderValuesWithoutChangingShowing() {
        Movie movie = new Movie(); movie.setIsShowing(true); movie.setId("local");
        OmdbMovieResponse response = new OmdbMovieResponse(); response.setTitle("The Matrix"); response.setImdbRating("8.7");
        response.setImdbVotes("1,234,567"); response.setMetascore("N/A"); response.setTotalSeasons("invalid"); response.setPoster("N/A");
        new OmdbMapper().apply(response, movie);
        assertThat(movie.getImdbRating()).isEqualByComparingTo(new BigDecimal("8.7")); assertThat(movie.getImdbVotes()).isEqualTo(1234567);
        assertThat(movie.getMetascore()).isNull(); assertThat(movie.getTotalSeasons()).isNull(); assertThat(movie.getPosterUrl()).isNull();
        assertThat(movie.getIsShowing()).isTrue(); assertThat(movie.getId()).isEqualTo("local");
    }
    @Test void searchEscapesRegexAndCombinesFilters() {
        service.search("a.*", "Action", "English", "1999", "movie", true);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class); verify(mongo).find(query.capture(), eq(Movie.class));
        String json = query.getValue().getQueryObject().toJson();
        assertThat(json).contains("genre", "language", "1999", "movie", "isShowing");
        assertThat(((java.util.regex.Pattern) query.getValue().getQueryObject().get("title")).pattern()).isEqualTo(java.util.regex.Pattern.quote("a.*"));
    }
    @Test void refreshNeverWritesLocalShowingOrCreationFields() {
        Movie movie = new Movie(); movie.setId("local"); movie.setImdbId("tt0133093"); movie.setIsShowing(true);
        when(repository.findById("local")).thenReturn(Optional.of(movie)); when(omdb.movie("tt0133093")).thenReturn(new OmdbMovieResponse());
        when(mongo.findAndModify(any(), any(), any(FindAndModifyOptions.class), eq(Movie.class))).thenReturn(movie);
        service.refresh("local"); ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).findAndModify(any(), update.capture(), any(FindAndModifyOptions.class), eq(Movie.class));
        assertThat(update.getValue().getUpdateObject().get("$set", org.bson.Document.class).keySet()).doesNotContain("isShowing", "createdAt");
    }
    @Test void unknownMovieReturns404() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireMovie("missing")).isInstanceOf(ApiException.class).hasMessage("Movie not found");
    }
}
