package com.cinemasaar.moviesservice.service;

import com.cinemasaar.moviesservice.repository.MovieRepository;
import com.cinemasaar.moviesservice.model.Movie;
import com.cinemasaar.moviesservice.dto.*;
import com.cinemasaar.moviesservice.client.OmdbClient;
import com.cinemasaar.moviesservice.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.dao.DuplicateKeyException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

// Handles catalogue queries and imported metadata without changing local cinema state.
@Service @RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final MongoTemplate mongo;
    private final OmdbClient omdb;
    private final OmdbMapper mapper;
    public List<Movie> getAllMovies() { return movieRepository.findAll(); }
    public Optional<Movie> getMovieById(String id) { return movieRepository.findById(id); }
    public Movie requireMovie(String id) { return getMovieById(id).orElseThrow(() -> ApiException.notFound("Movie")); }
    public List<Movie> getShowingMovies() { return movieRepository.findByIsShowingTrue(); }
    public Movie byImdb(String id) {
        OmdbClient.validateId(id);
        return movieRepository.findByImdbId(id).orElseThrow(() -> ApiException.notFound("Movie"));
    }
    public List<Movie> search(String title, String genre, String language, String year, String type, Boolean showing) {
        Query query = new Query();
        contains(query, "title", title); contains(query, "genre", genre); contains(query, "language", language);
        if (year != null && !year.isBlank()) query.addCriteria(Criteria.where("year").is(year));
        if (type != null && !type.isBlank()) query.addCriteria(Criteria.where("type").is(type));
        if (showing != null) query.addCriteria(Criteria.where("isShowing").is(showing));
        return mongo.find(query, Movie.class);
    }
    private void contains(Query query, String field, String value) {
        if (value != null && !value.isBlank()) {
            if (value.length() > 500) throw ApiException.badRequest("Search value too long");
            query.addCriteria(Criteria.where(field).regex(Pattern.quote(value.trim()), "i"));
        }
    }
    public Movie importMovie(String imdbId) {
        OmdbClient.validateId(imdbId);
        if (movieRepository.findByImdbId(imdbId).isPresent()) throw ApiException.conflict("Movie already imported");
        Movie movie = new Movie(); mapper.apply(omdb.movie(imdbId), movie);
        movie.setIsShowing(false); movie.setCreatedAt(Instant.now()); movie.setUpdatedAt(Instant.now());
        movie.setOmdbLastSyncedAt(Instant.now());
        try { return movieRepository.save(movie); }
        catch (DuplicateKeyException e) { throw ApiException.conflict("Movie already imported"); }
    }
    public Movie refresh(String id) {
        Movie movie = requireMovie(id);
        mapper.apply(omdb.movie(movie.getImdbId()), movie);
        movie.setOmdbLastSyncedAt(Instant.now()); movie.setUpdatedAt(Instant.now());
        // Partial update avoids overwriting an admin's concurrent showing change.
        Update update = new Update();
        update.set("title", movie.getTitle());
        update.set("year", movie.getYear());
        update.set("rated", movie.getRated());
        update.set("released", movie.getReleased());
        update.set("runtime", movie.getRuntime());
        update.set("genre", movie.getGenre());
        update.set("director", movie.getDirector());
        update.set("writer", movie.getWriter());
        update.set("actors", movie.getActors());
        update.set("plot", movie.getPlot());
        update.set("language", movie.getLanguage());
        update.set("country", movie.getCountry());
        update.set("awards", movie.getAwards());
        update.set("imdbRating", movie.getImdbRating());
        update.set("imdbVotes", movie.getImdbVotes());
        update.set("metascore", movie.getMetascore());
        update.set("boxOffice", movie.getBoxOffice());
        update.set("type", movie.getType());
        update.set("dvd", movie.getDvd());
        update.set("production", movie.getProduction());
        update.set("totalSeasons", movie.getTotalSeasons());
        update.set("imdbId", movie.getImdbId());
        update.set("posterUrl", movie.getPosterUrl()).set("websiteUrl", movie.getWebsiteUrl())
                .set("omdbLastSyncedAt", movie.getOmdbLastSyncedAt()).set("updatedAt", movie.getUpdatedAt());
        return update(id, update);
    }
    public Movie showing(String id, Boolean showing) { return update(id, new Update().set("isShowing", showing)); }
    public Movie edit(String id, MovieUpdateRequest request) {
        return update(id, new Update().set("title", request.title().trim()).set("rated", request.rated())
                .set("genre", request.genre()).set("language", request.language()).set("plot", request.plot()).set("isShowing", request.isShowing()));
    }
    private Movie update(String id, Update update) {
        Movie result = mongo.findAndModify(Query.query(Criteria.where("id").is(id)), update.set("updatedAt", Instant.now()),
                org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true), Movie.class);
        if (result == null) throw ApiException.notFound("Movie");
        return result;
    }
    public OmdbSearchResponse searchOmdb(String title, int page) { return omdb.search(title, page); }
}
