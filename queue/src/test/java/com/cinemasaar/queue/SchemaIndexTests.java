package com.cinemasaar.queue;

import com.cinemasaar.queue.admission.model.QueueEntry;
import com.cinemasaar.queue.checkout.model.Booking;
import com.cinemasaar.queue.payment.model.PaymentIntent;
import com.cinemasaar.queue.reservation.model.ShowtimeSeat;
import com.cinemasaar.queue.support.AbstractQueueIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.IndexInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The unique indexes are the last line of defence for the races below. If
 * auto-index-creation is ever switched off, these tests fail loudly.
 */
class SchemaIndexTests extends AbstractQueueIntegrationTest {

    @Test
    void uniqueIndexesGuardingTheConcurrencyInvariantsExist() {
        assertUniqueIndex(QueueEntry.class, "uq_showtime_user");
        assertUniqueIndex(QueueEntry.class, "uq_admission_token");
        assertUniqueIndex(ShowtimeSeat.class, "uq_showtime_seat");
        assertUniqueIndex(Booking.class, "uq_cart");
        assertUniqueIndex(PaymentIntent.class, "uq_booking");
    }

    private void assertUniqueIndex(Class<?> entity, String indexName) {
        assertThat(mongo.indexOps(entity).getIndexInfo())
                .filteredOn(index -> index.getName().equals(indexName))
                .singleElement()
                .satisfies(index -> assertThat(isUnique(index)).isTrue());
    }

    private boolean isUnique(IndexInfo index) {
        return index.isUnique();
    }
}
