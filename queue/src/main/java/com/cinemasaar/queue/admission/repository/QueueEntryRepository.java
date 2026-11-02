package com.cinemasaar.queue.admission.repository;

import com.cinemasaar.queue.admission.model.QueueEntry;
import com.cinemasaar.queue.admission.model.QueueStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QueueEntryRepository extends MongoRepository<QueueEntry, String> {

    Optional<QueueEntry> findByShowtimeIdAndUserId(String showtimeId, String userId);

    Optional<QueueEntry> findByAdmissionToken(String admissionToken);

    Optional<QueueEntry> findByCartId(String cartId);

    long countByShowtimeIdAndStatus(String showtimeId, QueueStatus status);

    long countByShowtimeIdAndStatusAndJoinedAtLessThan(
            String showtimeId, QueueStatus status, Instant joinedAt);

    List<QueueEntry> findTop100ByStatusAndCartIdIsNullAndAdmissionExpiresAtLessThanEqualOrderByAdmissionExpiresAtAsc(
            QueueStatus status, Instant now);
}
