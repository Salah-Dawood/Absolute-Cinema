package com.cinemasaar.moviesservice.dto;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

// Keeps the provider's response format separate from our MongoDB document.
@Getter @Setter @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
public class OmdbMovieResponse {
    @JsonProperty("Title") private String title;
    @JsonProperty("Year") private String year;
    @JsonProperty("Rated") private String rated;
    @JsonProperty("Released") private String released;
    @JsonProperty("Runtime") private String runtime;
    @JsonProperty("Genre") private String genre;
    @JsonProperty("Director") private String director;
    @JsonProperty("Writer") private String writer;
    @JsonProperty("Actors") private String actors;
    @JsonProperty("Plot") private String plot;
    @JsonProperty("Language") private String language;
    @JsonProperty("Country") private String country;
    @JsonProperty("Awards") private String awards;
    @JsonProperty("imdbRating") private String imdbRating;
    @JsonProperty("imdbVotes") private String imdbVotes;
    @JsonProperty("Metascore") private String metascore;
    @JsonProperty("BoxOffice") private String boxOffice;
    @JsonProperty("Poster") private String poster;
    @JsonProperty("Website") private String website;
    @JsonProperty("Type") private String type;
    @JsonProperty("DVD") private String dvd;
    @JsonProperty("Production") private String production;
    @JsonProperty("totalSeasons") private String totalSeasons;
    @JsonProperty("imdbID") private String imdbId;
    @JsonProperty("Response") private String response;
    @JsonProperty("Error") private String error;
}
