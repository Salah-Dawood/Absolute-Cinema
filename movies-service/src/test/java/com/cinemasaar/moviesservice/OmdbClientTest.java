package com.cinemasaar.moviesservice;

import com.cinemasaar.moviesservice.client.OmdbClient;
import com.cinemasaar.moviesservice.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;

class OmdbClientTest {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    OmdbClient client = new OmdbClient(builder, "test-only", "https://omdb.test");
    @Test void handlesMissingMovie() {
        server.expect(requestTo("https://omdb.test/?apikey=test-only&i=tt0133093&plot=full")).andRespond(withSuccess("{\"Response\":\"False\",\"Error\":\"Movie not found!\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.movie("tt0133093")).isInstanceOf(ApiException.class).hasMessageContaining("not found"); server.verify();
    }
    @Test void handlesRateLimitWithoutLeakingPayload() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("secret provider details"));
        assertThatThrownBy(() -> client.movie("tt0133093")).hasMessageContaining("rate limit").hasMessageNotContaining("secret");
    }
    @Test void validatesImdbBeforeCallingProvider() { assertThatThrownBy(() -> client.movie("invalid")).hasMessage("Invalid IMDb ID"); server.verify(); }
    @Test void rejectsMalformedProviderData() {
        server.expect(anything()).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.movie("tt0133093")).hasMessage("OMDb metadata unavailable");
    }
}
