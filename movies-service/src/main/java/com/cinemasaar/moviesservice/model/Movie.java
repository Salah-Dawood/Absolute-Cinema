package com.cinemasaar.moviesservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.math.BigDecimal;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString

// Maps this Java class to MongoDB movies collection
@Document(collection = "movies")
public class Movie {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    @Field("imdb_id")
    private String imdbId;
    @Field("title")
    private String title;
    @Field("year")
    private String year;
    @Field("rated")
    private String rated;
    @Field("released")
    private String released;
    @Field("runtime")
    private String runtime;
    @Field("genre")
    private String genre;
    @Field("director")
    private String director;
    @Field("writer")
    private String writer;
    @Field("actors")
    private String actors;
    @Field("plot")
    private String plot;
    @Field("language")
    private String language;
    @Field("country")
    private String country;
    @Field("awards")
    private String awards;

    @Field(value = "imdb_rating", targetType = org.springframework.data.mongodb.core.mapping.FieldType.DECIMAL128)
    private BigDecimal imdbRating;
    @Field("imdb_votes")
    private Integer imdbVotes;
    @Field("metascore")
    private Integer metascore;
    @Field("box_office")
    private String boxOffice;

    @Field("poster_url")
    private String posterUrl;
    @Field("website_url")
    private String websiteUrl;
    @Field("type")
    private String type;
    @Field("dvd")
    private String dvd;
    @Field("production")
    private String production;
    @Field("total_seasons")
    private Integer totalSeasons;

    @Field("is_showing")
    private Boolean isShowing;
    @Field("omdb_last_synced_at")
    private Instant omdbLastSyncedAt;
    @Field("created_at")
    private Instant createdAt;
    @Field("updated_at")
    private Instant updatedAt;
}