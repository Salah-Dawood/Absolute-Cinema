package com.cinemasaar.queue.support;

import com.cinemasaar.queue.admission.dto.QueueStatsResponse;
import com.cinemasaar.queue.admission.dto.QueueStatusResponse;
import com.cinemasaar.queue.admission.model.QueueStatus;
import com.cinemasaar.queue.checkout.dto.BookingResponse;
import com.cinemasaar.queue.payment.dto.PaymentResponse;
import com.cinemasaar.queue.reservation.dto.AddonSelection;
import com.cinemasaar.queue.reservation.dto.AddonSelectionRequest;
import com.cinemasaar.queue.reservation.dto.CartResponse;
import com.cinemasaar.queue.reservation.dto.CreateCartRequest;
import com.cinemasaar.queue.reservation.dto.SeatMapResponse;
import com.cinemasaar.queue.reservation.dto.SeedSeatsRequest;
import com.cinemasaar.queue.reservation.repository.BookingCartRepository;
import com.cinemasaar.queue.reservation.repository.ShowtimeSeatRepository;
import com.cinemasaar.queue.checkout.repository.BookingRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Boots the real service against a single-node MongoDB replica set so that
 * transactions behave exactly as they do in production.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.booking.cart-ttl=12s",
                "app.booking.admission-ttl=30s",
                "app.booking.admission-interval-ms=200",
                "app.booking.admission-expiry-interval-ms=200",
                "app.booking.cart-expiry-interval-ms=200",
                "app.booking.max-active-sessions-per-showtime=4"
        })
public abstract class AbstractQueueIntegrationTest {

    private static final MongoDBContainer MONGO =
            // Testcontainers 1.x starts MongoDBContainer as a single-node replica set by default.
            new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    static {
        MONGO.start();
        // The container advertises its own hostname as the replica set member,
        // which the host cannot resolve. directConnection keeps the driver on
        // the published port; transactions still work against the single node.
        String uri = "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017)
                + "/test?directConnection=true";
        System.out.println("[tests] MongoDB replica set at " + uri);
        System.setProperty("spring.data.mongodb.uri", uri);
    }

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected MongoTemplate mongo;

    @Autowired
    protected BookingRepository bookings;

    @Autowired
    protected BookingCartRepository carts;

    @Autowired
    protected ShowtimeSeatRepository seats;

    protected String newShowtimeId() {
        return "showtime-" + UUID.randomUUID();
    }

    protected String newUserId() {
        return "user-" + UUID.randomUUID();
    }

    protected HttpEntity<Void> as(String userId) {
        return new HttpEntity<>(headers(userId));
    }

    protected <T> HttpEntity<T> as(String userId, T body) {
        return new HttpEntity<>(body, headers(userId));
    }

    protected HttpHeaders headers(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (userId != null) {
            headers.set("X-User-Id", userId);
        }
        return headers;
    }

    protected void seedSeats(String showtimeId, List<String> rows, int seatsPerRow, String price) {
        SeedSeatsRequest request = new SeedSeatsRequest(rows, seatsPerRow, new BigDecimal(price));
        ResponseEntity<SeatMapResponse> response = rest.exchange(
                "/api/v1/admin/showtimes/{showtimeId}/seats",
                HttpMethod.POST,
                as(null, request),
                SeatMapResponse.class,
                showtimeId);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    protected QueueStatusResponse joinAndWaitForAdmission(String showtimeId, String userId) {
        ResponseEntity<QueueStatusResponse> joined = rest.exchange(
                "/api/v1/queue/{showtimeId}/join",
                HttpMethod.POST,
                as(userId),
                QueueStatusResponse.class,
                showtimeId);

        assertThat(joined.getStatusCode().value()).isEqualTo(200);

        Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
        while (Instant.now().isBefore(deadline)) {
            ResponseEntity<QueueStatusResponse> status = rest.exchange(
                    "/api/v1/queue/{showtimeId}/status",
                    HttpMethod.GET,
                    as(userId),
                    QueueStatusResponse.class,
                    showtimeId);

            if (status.getBody() != null
                    && status.getBody().status() == QueueStatus.ADMITTED
                    && status.getBody().admissionToken() != null) {
                return status.getBody();
            }
            sleep(100);
        }
        fail("User %s was not admitted for showtime %s", userId, showtimeId);
        return null;
    }

    protected QueueStatusResponse queueStatus(String showtimeId, String userId) {
        return rest.exchange(
                "/api/v1/queue/{showtimeId}/status",
                HttpMethod.GET,
                as(userId),
                QueueStatusResponse.class,
                showtimeId).getBody();
    }

    protected QueueStatsResponse queueStats(String showtimeId) {
        return rest.exchange(
                "/api/v1/queue/{showtimeId}/stats",
                HttpMethod.GET,
                as(null),
                QueueStatsResponse.class,
                showtimeId).getBody();
    }

    protected CartResponse createCart(String userId, String admissionToken) {
        ResponseEntity<CartResponse> response = rest.exchange(
                "/api/v1/carts",
                HttpMethod.POST,
                as(userId, new CreateCartRequest(admissionToken)),
                CartResponse.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return response.getBody();
    }

    protected ResponseEntity<CartResponse> addSeat(String cartId, String userId, String seatId) {
        return rest.exchange(
                "/api/v1/carts/{cartId}/seats/{seatId}",
                HttpMethod.POST,
                as(userId),
                CartResponse.class,
                cartId,
                seatId);
    }

    protected ResponseEntity<CartResponse> setAddons(String cartId, String userId, String seatId,
                                                     List<AddonSelection> selections) {
        return rest.exchange(
                "/api/v1/carts/{cartId}/seats/{seatId}/addons",
                HttpMethod.PUT,
                as(userId, new AddonSelectionRequest(selections)),
                CartResponse.class,
                cartId,
                seatId);
    }

    protected CartResponse getCart(String cartId, String userId) {
        return rest.exchange(
                "/api/v1/carts/{cartId}",
                HttpMethod.GET,
                as(userId),
                CartResponse.class,
                cartId).getBody();
    }

    protected ResponseEntity<BookingResponse> checkout(String cartId, String userId) {
        return rest.exchange(
                "/api/v1/carts/{cartId}/checkout",
                HttpMethod.POST,
                as(userId),
                BookingResponse.class,
                cartId);
    }

    protected ResponseEntity<PaymentResponse> pay(String intentId, String userId,
                                                  String idempotencyKey) {
        HttpHeaders headers = headers(userId);
        headers.set("Idempotency-Key", idempotencyKey);
        return rest.exchange(
                "/api/v1/payments/{intentId}/pay",
                HttpMethod.POST,
                new HttpEntity<>("{\"method\":\"CARD\"}", headers),
                PaymentResponse.class,
                intentId);
    }

    protected BookingResponse booking(String bookingId, String userId) {
        return rest.exchange(
                "/api/v1/bookings/{bookingId}",
                HttpMethod.GET,
                as(userId),
                BookingResponse.class,
                bookingId).getBody();
    }

    protected SeatMapResponse seatMap(String showtimeId) {
        return rest.exchange(
                "/api/v1/showtimes/{showtimeId}/seats",
                HttpMethod.GET,
                as(null),
                SeatMapResponse.class,
                showtimeId).getBody();
    }

    protected long activeSessions(String showtimeId) {
        return queueStats(showtimeId).activeSessions();
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    protected void await(String description, java.util.function.BooleanSupplier condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(20));
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            sleep(150);
        }
        fail("Timed out waiting for: %s", description);
    }
}
