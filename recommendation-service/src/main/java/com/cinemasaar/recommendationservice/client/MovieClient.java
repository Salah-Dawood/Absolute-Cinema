package com.cinemasaar.recommendationservice.client;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import com.cinemasaar.recommendationservice.dto.CatalogueMovie;
import com.cinemasaar.recommendationservice.exception.CatalogueUnavailableException;
import java.util.*;

// Reads the public movies REST contract, never another service's database.
@Component
public class MovieClient {
    private final RestClient client;
    public MovieClient(RestClient.Builder builder, @Value("${movie.service.url}") String baseUrl) { client = builder.baseUrl(baseUrl).build(); }
    public List<CatalogueMovie> showing() {
        try {
            CatalogueMovie[] movies = client.get().uri("/api/movies/showing").retrieve().body(CatalogueMovie[].class);
            if (movies == null) throw new CatalogueUnavailableException();
            return Arrays.stream(movies).filter(m -> m != null && m.id() != null && !m.id().isBlank()
                    && m.title() != null && Boolean.TRUE.equals(m.isShowing())).toList();
        } catch (Exception e) { throw new CatalogueUnavailableException(); }
    }
}
