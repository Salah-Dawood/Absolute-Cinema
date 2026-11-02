package com.cinemasaar.recommendationservice;

import com.cinemasaar.recommendationservice.client.*;
import com.cinemasaar.recommendationservice.dto.*;
import com.cinemasaar.recommendationservice.service.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class RecommendationServiceTest {
    MovieClient movies = mock(MovieClient.class); OpenAiClient ai = mock(OpenAiClient.class);
    RecommendationService service = new RecommendationService(movies, ai);
    PreferencesRequest request = new PreferencesRequest("happy", List.of("Comedy"), "English", "low", 120, null, 2);
    CatalogueMovie action = new CatalogueMovie("a", "Action", "Action", "Arabic", "150 min", "plot", true);
    CatalogueMovie comedy = new CatalogueMovie("b", "Comedy", "Comedy", "English", "100 min", "plot", true);
    @BeforeEach void setup() { when(movies.showing()).thenReturn(List.of(action, comedy)); }
    @Test void emptyCatalogueDoesNotCallAi() {
        when(movies.showing()).thenReturn(List.of()); assertThat(service.recommend(request).source()).isEqualTo("EMPTY_CATALOGUE"); verifyNoInteractions(ai);
    }
    @Test void providerFailureUsesStablePreferenceRanking() {
        when(ai.recommend(any(), any())).thenThrow(new IllegalStateException("timeout"));
        RecommendationResponse result = service.recommend(request); assertThat(result.source()).isEqualTo("FALLBACK");
        assertThat(result.recommendations().get(0).movie().id()).isEqualTo("b"); assertThat(service.recommend(request)).isEqualTo(result);
    }
    @Test void inventedIdTriggersFallback() {
        when(ai.recommend(any(), any())).thenReturn(new AiSelection(List.of(new AiSelection.Item("invented", "reason"))));
        RecommendationResponse result = service.recommend(request); assertThat(result.source()).isEqualTo("FALLBACK");
        assertThat(result.recommendations()).allMatch(r -> Set.of("a", "b").contains(r.movie().id()));
    }
    @Test void duplicateIdsTriggerFallback() {
        when(ai.recommend(any(), any())).thenReturn(new AiSelection(List.of(new AiSelection.Item("a", "one"), new AiSelection.Item("a", "two"))));
        assertThat(service.recommend(request).source()).isEqualTo("FALLBACK");
    }
    @Test void validResponseUsesActualCatalogueObject() {
        when(ai.recommend(any(), any())).thenReturn(new AiSelection(List.of(new AiSelection.Item("b", "Matches your comedy preference"))));
        RecommendationResponse result = service.recommend(request); assertThat(result.source()).isEqualTo("OPENAI");
        assertThat(result.recommendations().get(0).movie()).isSameAs(comedy);
    }
    @Test void missingExplanationTriggersFallback() {
        when(ai.recommend(any(), any())).thenReturn(new AiSelection(List.of(new AiSelection.Item("b", ""))));
        assertThat(service.recommend(request).source()).isEqualTo("FALLBACK");
    }
    @Test void catalogueFailureDoesNotInventRecommendations() {
        when(movies.showing()).thenThrow(new com.cinemasaar.recommendationservice.exception.CatalogueUnavailableException());
        assertThatThrownBy(() -> service.recommend(request)).isInstanceOf(com.cinemasaar.recommendationservice.exception.CatalogueUnavailableException.class); verifyNoInteractions(ai);
    }
}
