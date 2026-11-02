package com.cinemasaar.queue.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(
        int maxActiveSessionsPerShowtime,
        Duration admissionTtl,
        Duration cartTtl,
        int maxSeatsPerCart,
        int maxAddonsPerSeat,
        int admissionBatchSize,
        Duration seatOperationTimeout,
        Pool pool
) {
    public record Pool(int coreSize, int maxSize, int queueCapacity) {
    }
}
