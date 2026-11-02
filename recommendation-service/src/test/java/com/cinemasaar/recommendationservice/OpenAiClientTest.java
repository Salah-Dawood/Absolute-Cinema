package com.cinemasaar.recommendationservice;

import com.cinemasaar.recommendationservice.client.*;
import com.cinemasaar.recommendationservice.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;

class OpenAiClientTest {
    RestClient.Builder builder = RestClient.builder(); MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    OpenAiClient client = new OpenAiClient(builder, new ObjectMapper(), "test-only", "test-model", "https://ai.test/v1");
    PreferencesRequest preferences = new PreferencesRequest(null, null, null, null, null, null, 1);
    @Test void sendsStrictSchemaAndParsesStructuredResponse() {
        String body = "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"{\\\"recommendations\\\":[{\\\"movieId\\\":\\\"m\\\",\\\"explanation\\\":\\\"A match\\\"}]}\"}]}]}";
        server.expect(requestTo("https://ai.test/v1/responses")).andExpect(header("Authorization", "Bearer test-only"))
                .andExpect(jsonPath("$.text.format.strict").value(true)).andExpect(jsonPath("$.store").value(false))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThat(client.recommend(preferences, List.of()).recommendations().get(0).movieId()).isEqualTo("m"); server.verify();
    }
    @Test void rejectsIncompleteResponse() {
        server.expect(anything()).andRespond(withSuccess("{\"status\":\"incomplete\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.recommend(preferences, List.of())).isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsMalformedJson() {
        server.expect(anything()).andRespond(withSuccess("invalid", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.recommend(preferences, List.of())).isInstanceOf(RuntimeException.class);
    }
}
