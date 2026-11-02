package com.cinemasaar.recommendationservice.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.cinemasaar.recommendationservice.client.*;
import com.cinemasaar.recommendationservice.dto.*;
import java.util.*;

// Validates all AI choices against the current catalogue and provides stable local ranking.
@Service @RequiredArgsConstructor
public class RecommendationService {
    private final MovieClient movies;
    private final OpenAiClient ai;
    public RecommendationResponse recommend(PreferencesRequest request) {
        List<CatalogueMovie> catalogue = movies.showing();
        if (catalogue.isEmpty()) return new RecommendationResponse("EMPTY_CATALOGUE", List.of());
        // Cap provider input after preference ranking; the returned movie objects always come from the catalogue.
        List<CatalogueMovie> candidates = catalogue.stream().sorted(Comparator
                .comparingInt((CatalogueMovie movie) -> score(movie, request)).reversed().thenComparing(CatalogueMovie::id)).limit(50).toList();
        try {
            AiSelection selection = ai.recommend(request, candidates);
            Map<String,CatalogueMovie> allowed = new HashMap<>(); candidates.forEach(m -> allowed.put(m.id(), m));
            if (selection == null || selection.recommendations() == null || selection.recommendations().isEmpty()
                    || selection.recommendations().size() > request.resultLimit()) throw new IllegalArgumentException("Invalid AI selection");
            Set<String> used = new HashSet<>();
            List<RecommendationResponse.Item> result = new ArrayList<>();
            for (AiSelection.Item item : selection.recommendations()) {
                if (item == null || !allowed.containsKey(item.movieId()) || !used.add(item.movieId())
                        || item.explanation() == null || item.explanation().isBlank() || item.explanation().length() > 600)
                    throw new IllegalArgumentException("Invalid AI selection");
                result.add(new RecommendationResponse.Item(allowed.get(item.movieId()), item.explanation()));
            }
            return new RecommendationResponse("OPENAI", result);
        } catch (RuntimeException e) {
            return new RecommendationResponse("FALLBACK", candidates.stream().limit(request.resultLimit())
                    .map(m -> new RecommendationResponse.Item(m, "Selected from currently showing movies using genre, language and duration preferences where available.")).toList());
        }
    }
    private int score(CatalogueMovie movie, PreferencesRequest request) {
        int score = 0;
        if (request.genres() != null) for (String genre : request.genres()) if (contains(movie.genre(), genre)) score += 3;
        if (contains(movie.language(), request.language())) score += 4;
        if (request.maxDurationMinutes() != null && movie.runtime() != null) {
            try { int minutes = Integer.parseInt(movie.runtime().replace(" min", "").trim());
                score += minutes <= request.maxDurationMinutes() ? 2 : -2;
            } catch (NumberFormatException ignored) { /* Unknown duration receives no bonus. */ }
        }
        return score;
    }
    private boolean contains(String value, String preference) {
        return value != null && preference != null && !preference.isBlank()
                && value.toLowerCase(Locale.ROOT).contains(preference.toLowerCase(Locale.ROOT));
    }
}
