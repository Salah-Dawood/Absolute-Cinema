package com.cinemasaar.queue;

import com.cinemasaar.queue.admission.model.QueueStatus;
import com.cinemasaar.queue.checkout.model.BookingStatus;
import com.cinemasaar.queue.checkout.dto.BookingResponse;
import com.cinemasaar.queue.common.ApiError;
import com.cinemasaar.queue.payment.dto.PaymentResponse;
import com.cinemasaar.queue.payment.model.PaymentStatus;
import com.cinemasaar.queue.reservation.dto.AddonSelection;
import com.cinemasaar.queue.reservation.dto.CartResponse;
import com.cinemasaar.queue.reservation.dto.SeatMapResponse;
import com.cinemasaar.queue.reservation.model.CartStatus;
import com.cinemasaar.queue.reservation.model.SeatStatus;
import com.cinemasaar.queue.reservation.model.ShowtimeSeat;
import com.cinemasaar.queue.support.AbstractQueueIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class BookingFlowTests extends AbstractQueueIntegrationTest {

    @Test
    void seatsAreOnlyBookedAfterPaymentConfirms() {
        String showtimeId = newShowtimeId();
        seedSeats(showtimeId, List.of("A", "B"), 3, "12.50");

        String userId = newUserId();
        CartResponse cart = createCart(userId,
                joinAndWaitForAdmission(showtimeId, userId).admissionToken());

        assertThat(addSeat(cart.cartId(), userId, "A1").getStatusCode().value()).isEqualTo(200);
        assertThat(addSeat(cart.cartId(), userId, "A2").getStatusCode().value()).isEqualTo(200);

        ResponseEntity<CartResponse> withAddons = setAddons(cart.cartId(), userId, "A1",
                List.of(new AddonSelection("popcorn", 2)));
        assertThat(withAddons.getStatusCode().value()).isEqualTo(200);
        assertThat(withAddons.getBody().seatTotal()).isEqualByComparingTo("25.00");
        assertThat(withAddons.getBody().addonTotal()).isEqualByComparingTo("10.00");
        assertThat(withAddons.getBody().total()).isEqualByComparingTo("35.00");

        // Holding a seat is not booking it.
        assertThat(seatStatus(showtimeId, "A1")).isEqualTo(SeatStatus.HELD);

        ResponseEntity<BookingResponse> checkoutResponse = checkout(cart.cartId(), userId);
        assertThat(checkoutResponse.getStatusCode().value()).isEqualTo(201);

        BookingResponse pending = checkoutResponse.getBody();
        assertThat(pending.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(pending.payment().status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(pending.total()).isEqualByComparingTo("35.00");

        ResponseEntity<PaymentResponse> paid = pay(
                pending.payment().paymentIntentId(), userId, UUID.randomUUID().toString());
        assertThat(paid.getStatusCode().value()).isEqualTo(200);
        assertThat(paid.getBody().status()).isEqualTo(PaymentStatus.PAID);

        BookingResponse confirmed = booking(pending.bookingId(), userId);
        assertThat(confirmed.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(confirmed.payment().status()).isEqualTo(PaymentStatus.PAID);
        assertThat(confirmed.confirmedAt()).isNotNull();

        assertThat(seatStatus(showtimeId, "A1")).isEqualTo(SeatStatus.BOOKED);
        assertThat(seatStatus(showtimeId, "A2")).isEqualTo(SeatStatus.BOOKED);
        assertThat(seatStatus(showtimeId, "A3")).isEqualTo(SeatStatus.AVAILABLE);

        assertThat(getCart(cart.cartId(), userId).status()).isEqualTo(CartStatus.COMPLETED);
        assertThat(queueStatus(showtimeId, userId).status()).isEqualTo(QueueStatus.COMPLETED);
        assertThat(activeSessions(showtimeId)).isZero();
    }

    @Test
    void payingAgainWithTheSameKeyReturnsTheSamePaidPayment() {
        String showtimeId = newShowtimeId();
        seedSeats(showtimeId, List.of("A"), 2, "9.00");

        String userId = newUserId();
        CartResponse cart = createCart(userId,
                joinAndWaitForAdmission(showtimeId, userId).admissionToken());
        addSeat(cart.cartId(), userId, "A1");

        BookingResponse pending = checkout(cart.cartId(), userId).getBody();
        String key = UUID.randomUUID().toString();

        PaymentResponse first = pay(pending.payment().paymentIntentId(), userId, key).getBody();
        ResponseEntity<PaymentResponse> second = pay(
                pending.payment().paymentIntentId(), userId, key);

        assertThat(first.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(second.getStatusCode().value()).isEqualTo(200);
        assertThat(second.getBody().status()).isEqualTo(PaymentStatus.PAID);
        assertThat(second.getBody().paymentIntentId()).isEqualTo(first.paymentIntentId());
        assertThat(booking(pending.bookingId(), userId).status())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void checkoutIsAllOrNothingWhenOneHoldWasLost() {
        String showtimeId = newShowtimeId();
        seedSeats(showtimeId, List.of("A"), 3, "12.00");

        String userId = newUserId();
        CartResponse cart = createCart(userId,
                joinAndWaitForAdmission(showtimeId, userId).admissionToken());
        addSeat(cart.cartId(), userId, "A1");
        addSeat(cart.cartId(), userId, "A2");

        // Simulate A2 being released by an expiry/repair path while A1 is still held.
        mongo.updateFirst(
                Query.query(Criteria.where("showtimeId").is(showtimeId).and("seatId").is("A2")),
                new Update()
                        .set("status", SeatStatus.AVAILABLE)
                        .unset("cartId")
                        .unset("heldBy")
                        .unset("holdExpiresAt"),
                ShowtimeSeat.class);

        ResponseEntity<ApiError> response = rest.exchange(
                "/api/v1/carts/{cartId}/checkout",
                HttpMethod.POST,
                as(userId),
                ApiError.class,
                cart.cartId());

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SEATS_LOST");
        assertThat((List<String>) response.getBody().details()).containsExactly("A2");

        // No booking was created and no seat was silently booked.
        assertThat(bookings.findByCartId(cart.cartId())).isEmpty();
        assertThat(carts.findById(cart.cartId()).orElseThrow().getStatus())
                .isEqualTo(CartStatus.OPEN);
        assertThat(seatStatus(showtimeId, "A1")).isEqualTo(SeatStatus.HELD);
    }

    @Test
    void anAdmissionTokenCannotCreateASecondCart() {
        String showtimeId = newShowtimeId();
        seedSeats(showtimeId, List.of("A"), 2, "9.00");

        String userId = newUserId();
        String token = joinAndWaitForAdmission(showtimeId, userId).admissionToken();
        createCart(userId, token);

        ResponseEntity<ApiError> second = rest.exchange(
                "/api/v1/carts",
                HttpMethod.POST,
                as(userId, new com.cinemasaar.queue.reservation.dto.CreateCartRequest(token)),
                ApiError.class);

        assertThat(second.getStatusCode().value()).isEqualTo(403);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().code()).isEqualTo("ADMISSION_REQUIRED");
    }

    private SeatStatus seatStatus(String showtimeId, String seatId) {
        SeatMapResponse map = seatMap(showtimeId);
        return map.rows().stream()
                .flatMap(row -> row.seats().stream())
                .filter(seat -> seat.seatId().equals(seatId))
                .findFirst()
                .orElseThrow()
                .status();
    }
}
