package com.cinemasaar.recommendationservice;

import com.cinemasaar.recommendationservice.client.MovieClient;
import com.cinemasaar.recommendationservice.exception.CatalogueUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;

class MovieClientTest {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    MovieClient client = new MovieClient(builder, "http://catalogue.test");
    @Test void onlyReturnsValidShowingMovies() {
        server.expect(requestTo("http://catalogue.test/api/movies/showing")).andRespond(withSuccess(
                "[{\"id\":\"one\",\"title\":\"Showing\",\"isShowing\":true},{\"id\":\"two\",\"title\":\"Hidden\",\"isShowing\":false}]", MediaType.APPLICATION_JSON));
        assertThat(client.showing()).hasSize(1).first().extracting(m -> m.id()).isEqualTo("one");
    }
    @Test void translatesDownstreamErrorsToCatalogueFailure() {
        server.expect(anything()).andRespond(withServerError());
        assertThatThrownBy(client::showing).isInstanceOf(CatalogueUnavailableException.class);
    }
}
