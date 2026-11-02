package com.cinemasaar.moviesservice.client;

import com.cinemasaar.moviesservice.dto.*;
import com.cinemasaar.moviesservice.exception.ApiException;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.*;
import org.springframework.http.HttpStatus;

// Retrieves metadata from OMDb without scraping IMDb.
@Component
public class OmdbClient {
    private final RestClient client;
    private final String key;
    public OmdbClient(RestClient.Builder builder, @Value("${omdb.api.key:}") String key,
            @Value("${omdb.base-url:https://www.omdbapi.com}") String url) {
        this.key = key; this.client = builder.baseUrl(url).build();
    }
    public OmdbMovieResponse movie(String imdbId) {
        validateId(imdbId); configured();
        try {
            OmdbMovieResponse response = client.get().uri(b -> b.path("/").queryParam("apikey", key)
                    .queryParam("i", imdbId).queryParam("plot", "full").build()).retrieve().body(OmdbMovieResponse.class);
            if (response == null) throw unavailable();
            check(response.getResponse(), response.getError());
            if (!imdbId.equals(response.getImdbId()) || response.getTitle() == null || "N/A".equals(response.getTitle())) throw unavailable();
            return response;
        } catch (RestClientException e) { throw providerFailure(e); }
    }
    public OmdbSearchResponse search(String title, int page) {
        if (title == null || title.isBlank() || title.length() > 500 || page < 1 || page > 100)
            throw ApiException.badRequest("Provide a title and page between 1 and 100");
        configured();
        try {
            OmdbSearchResponse response = client.get().uri(b -> b.path("/").queryParam("apikey", key)
                    .queryParam("s", title).queryParam("page", page).build()).retrieve().body(OmdbSearchResponse.class);
            if (response == null) throw unavailable();
            check(response.response(), response.error()); return response;
        } catch (RestClientException e) { throw providerFailure(e); }
    }
    public static void validateId(String id) {
        if (id == null || !id.matches("tt[0-9]{7,10}")) throw ApiException.badRequest("Invalid IMDb ID");
    }
    private void configured() { if (key.isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OMDb is not configured"); }
    private void check(String response, String error) {
        if ("True".equalsIgnoreCase(response)) return;
        String message = error == null ? "" : error.toLowerCase(java.util.Locale.ROOT);
        if (message.contains("not found")) throw ApiException.notFound("OMDb movie");
        if (message.contains("limit")) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OMDb rate limit reached; retry later");
        throw unavailable();
    }
    private ApiException providerFailure(RestClientException e) {
        if (e instanceof RestClientResponseException response && response.getStatusCode().value() == 429)
            return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "OMDb rate limit reached; retry later");
        return unavailable();
    }
    private ApiException unavailable() { return new ApiException(HttpStatus.BAD_GATEWAY, "OMDb metadata unavailable"); }
}
