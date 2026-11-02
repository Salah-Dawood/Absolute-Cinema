package com.cinemasaar.queue;

import com.cinemasaar.queue.admission.model.QueueStatus;
import com.cinemasaar.queue.checkout.dto.BookingResponse;
import com.cinemasaar.queue.checkout.model.BookingStatus;
import com.cinemasaar.queue.common.ApiError;
import com.cinemasaar.queue.payment.dto.PaymentResponse;
import com.cinemasaar.queue.payment.model.PaymentStatus;
import com.cinemasaar.queue.reservation.dto.CartResponse;
import com.cinemasaar.queue.reservation.dto.SeatMapResponse;
import com.cinemasaar.queue.reservation.model.CartStatus;
import com.cinemasaar.queue.reservation.model.SeatStatus;
import com.cinemasaar.queue.support.AbstractQueueIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The 15-minute hold is compressed to a few seconds here. The point is that
 * expiry is driven by the same scheduler and the same transactions as production.
 */
class CartExpiryTests extends AbstractQueueIntegrationTest {

    @Test
    void anAbandonedCartReleasesItsSeatAndAdmissionSlot() {
        String showtimeId = newShowtimeId();
        seedSeats(showtimeId, List.of("A"), 3, "11.00");

        String userId = newUserId();
        CartResponse cart = createCart(userId,
                joinAndWaitForAdmission(showtimeId, userId).admissionToken());
        addSeat(cart.cartId(), userId, "A1");

        assertThat(seatStatus(showtimeId, "A1")).isEqualTo(SeatStatus.HELD);

        await("cart to expire", () ->
                getCart(cart.cartId(), userId).status() == CartStatus.EXPIRED);

        assertThat(seatStatus(showtimeId, "A1")).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(queueStatus(showtimeId, userId).status()).isEqualTo(QueueStatus.COMPLETED);
        assertThat(activeSessions(showtimeId)).isZero();

        // The released seat is immediately available to the next admitted user.
        String other = newUserId();
        CartResponse otherCart = createCart(other,
                joinAndWaitForAdmission(showtimeId, other).admissionToken());
        ResponseEntity<CartResponse> rehold = addSeat(otherCart.cartId(), other, "A1");

        assertThat(rehold.getStatusCode().value()).isEqualTo(200);
        assertThat(rehold.getBody().items()).hasSize(1);
    }

    @Test
    void aCheckedOutCartThatIsNeverPaidExpiresAndCancelsItsPayment() {
        String showtimeId = newShowtimeId();
        seedSeats(showtimeId, List.of("A"), 2, "10.00");

        String userId = newUserId();
        CartResponse cart = createCart(userId,
                joinAndWaitForAdmission(showtimeId, userId).admissionToken());
        addSeat(cart.cartId(), userId, "A1");

        BookingResponse pending = checkout(cart.cartId(), userId).getBody();
        assertThat(pending.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);

        await("booking to expire", () ->
                booking(pending.bookingId(), userId).status() == BookingStatus.EXPIRED);

        assertThat(getCart(cart.cartId(), userId).status()).isEqualTo(CartStatus.EXPIRED);
        assertThat(seatStatus(showtimeId, "A1")).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(paymentStatus(pending.payment().paymentIntentId(), userId))
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat(activeSessions(showtimeId)).isZero();

        // Paying after the hold expired must not revive the booking.
        HttpHeaders headers = headers(userId);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        ResponseEntity<ApiError> latePayment = rest.exchange(
                "/api/v1/payments/{intentId}/pay",
                HttpMethod.POST,
                new HttpEntity<>(new com.cinemasaar.queue.payment.dto.PayRequest("CARD"), headers),
                ApiError.class,
                pending.payment().paymentIntentId());

        assertThat(latePayment.getStatusCode().value()).isEqualTo(409);
        assertThat(latePayment.getBody()).isNotNull();
        assertThat(latePayment.getBody().code()).isEqualTo("PAYMENT_NOT_PAYABLE");
        assertThat(booking(pending.bookingId(), userId).status())
                .isEqualTo(BookingStatus.EXPIRED);
        assertThat(seatStatus(showtimeId, "A1")).isEqualTo(SeatStatus.AVAILABLE);
    }

    private PaymentStatus paymentStatus(String intentId, String userId) {
        ResponseEntity<PaymentResponse> response = rest.exchange(
                "/api/v1/payments/{intentId}",
                HttpMethod.GET,
                as(userId),
                PaymentResponse.class,
                intentId);
        return response.getBody() == null ? null : response.getBody().status();
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
