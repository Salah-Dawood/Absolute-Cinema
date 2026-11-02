package com.cinemasaar.queue;

import com.cinemasaar.queue.common.ApiError;
import com.cinemasaar.queue.reservation.dto.CartResponse;
import com.cinemasaar.queue.reservation.model.BookingCart;
import com.cinemasaar.queue.support.AbstractQueueIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two races the service has to survive: two users clicking the same seat,
 * and one admission token being used twice at the same moment.
 */
class SeatConcurrencyTests extends AbstractQueueIntegrationTest {

    @Test
    void onlyOneOfTwoConcurrentClicksOnTheSameSeatWins() throws Exception {
        String showtimeId = newShowtimeId();
        seedSeats(showtimeId, List.of("A"), 4, "12.00");

        String alice = newUserId();
        String bob = newUserId();
        CartResponse aliceCart = createCart(alice,
                joinAndWaitForAdmission(showtimeId, alice).admissionToken());
        CartResponse bobCart = createCart(bob,
                joinAndWaitForAdmission(showtimeId, bob).admissionToken());

        List<ResponseEntity<String>> results = race(
                () -> addSeatRaw(aliceCart.cartId(), alice, "A1"),
                () -> addSeatRaw(bobCart.cartId(), bob, "A1"));

        List<String> responses = results.stream()
                .map(r -> r.getStatusCode().value() + " " + r.getBody())
                .toList();

        long winners = results.stream()
                .filter(r -> r.getStatusCode().value() == 200)
                .count();
        long conflicts = results.stream()
                .filter(r -> r.getStatusCode().value() == 409)
                .count();

        assertThat(winners).as("responses: %s", responses)
                .isEqualTo(1);
        assertThat(conflicts).as("responses: %s", responses)
                .isEqualTo(1);

        CartResponse aliceAfter = getCart(aliceCart.cartId(), alice);
        CartResponse bobAfter = getCart(bobCart.cartId(), bob);
        assertThat(aliceAfter.items().size() + bobAfter.items().size())
                .as("the seat is in exactly one cart")
                .isEqualTo(1);

        boolean aliceLost = aliceAfter.items().isEmpty();
        CartResponse loserCart = aliceLost ? aliceAfter : bobAfter;
        String loser = aliceLost ? alice : bob;

        ResponseEntity<ApiError> retry = rest.exchange(
                "/api/v1/carts/{cartId}/seats/{seatId}",
                HttpMethod.POST,
                as(loser),
                ApiError.class,
                loserCart.cartId(),
                "A1");

        assertThat(retry.getStatusCode().value()).isEqualTo(409);
        assertThat(retry.getBody()).isNotNull();
        assertThat(retry.getBody().code()).isEqualTo("SEAT_UNAVAILABLE");
        assertThat(retry.getBody().details()).isEqualTo("A1");
    }

    @Test
    void oneAdmissionTokenCanOnlyCreateOneCartUnderConcurrentRequests() throws Exception {
        String showtimeId = newShowtimeId();
        seedSeats(showtimeId, List.of("A"), 3, "10.00");

        String userId = newUserId();
        var admission = joinAndWaitForAdmission(showtimeId, userId);
        String token = admission.admissionToken();

        List<ResponseEntity<String>> results = race(
                () -> createCartRaw(userId, token),
                () -> createCartRaw(userId, token));

        long created = results.stream()
                .filter(r -> r.getStatusCode().value() == 201)
                .count();
        assertThat(created).as("only one cart may consume the token").isEqualTo(1);

        ResponseEntity<String> loser = results.stream()
                .filter(r -> r.getStatusCode().value() != 201)
                .findFirst()
                .orElseThrow();
        assertThat(loser.getStatusCode().value())
                .as("body: %s", loser.getBody())
                .isIn(403, 409, 410);

        long cartsForEntry = mongo.count(
                Query.query(Criteria.where("queueEntryId").is(admission.queueEntryId())),
                BookingCart.class);
        assertThat(cartsForEntry).isEqualTo(1);
    }

    private ResponseEntity<String> addSeatRaw(String cartId, String userId, String seatId) {
        return rest.exchange(
                "/api/v1/carts/{cartId}/seats/{seatId}",
                HttpMethod.POST,
                as(userId),
                String.class,
                cartId,
                seatId);
    }

    private ResponseEntity<String> createCartRaw(String userId, String token) {
        return rest.exchange(
                "/api/v1/carts",
                HttpMethod.POST,
                as(userId, new com.cinemasaar.queue.reservation.dto.CreateCartRequest(token)),
                String.class);
    }

    @SafeVarargs
    private final List<ResponseEntity<String>> race(
            Callable<ResponseEntity<String>>... calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.length);
        CountDownLatch ready = new CountDownLatch(calls.length);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
            for (Callable<ResponseEntity<String>> call : calls) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await(10, TimeUnit.SECONDS);
                    return call.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            List<ResponseEntity<String>> results = new ArrayList<>();
            for (Future<ResponseEntity<String>> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}
