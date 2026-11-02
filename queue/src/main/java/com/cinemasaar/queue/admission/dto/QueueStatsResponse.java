package com.cinemasaar.queue.admission.dto;

public record QueueStatsResponse(
        String showtimeId,
        long waiting,
        long activeSessions,
        int capacity
) {
}
