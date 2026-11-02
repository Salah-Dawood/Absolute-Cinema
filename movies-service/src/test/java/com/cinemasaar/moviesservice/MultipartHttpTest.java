package com.cinemasaar.moviesservice;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

// Uses embedded Tomcat because standalone MockMvc does not parse multipart boundaries.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultipartHttpTest {
    static final MongoServer mongo = new MongoServer(new MemoryBackend());
    static final String uri = mongo.bindAndGetConnectionString() + "/multipart_http_test";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> uri);
    }

    @Value("${local.server.port}")
    int port;

    @AfterAll
    static void close() { mongo.shutdownNow(); }

    @Test
    void missingMultipartBoundaryReturns400() throws Exception {
        HttpResponse<String> response = post("multipart/form-data", "not JSON");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"error\":\"BAD_REQUEST\"", "\"message\":\"Invalid request\"", "\"path\":\"/api/seats\"")
                .doesNotContain("FileUploadException", "Failed to parse", "SERVICE_UNAVAILABLE");
    }

    @Test
    void validMultipartStillReturns415() throws Exception {
        String body = "--cinema-boundary\r\n"
                + "Content-Disposition: form-data; name=\"example\"\r\n\r\nvalue\r\n"
                + "--cinema-boundary--\r\n";
        HttpResponse<String> response = post("multipart/form-data; boundary=cinema-boundary", body);

        assertThat(response.statusCode()).isEqualTo(415);
        assertThat(response.body()).contains("Content-Type must be application/json");
    }

    private HttpResponse<String> post(String contentType, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/seats"))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
