package com.cinemasaar.queue.admission.dto;

import com.cinemasaar.queue.admission.model.QueueStatus;
import java.time.Instant;

public record QueueStatusResponse(
        String queueEntryId,
        QueueStatus status,
        long position,
        long activeSessions,
        int capacity,
        String admissionToken,
        Instant admissionExpiresAt,
        String cartId,
        Instant joinedAt
) {
}
