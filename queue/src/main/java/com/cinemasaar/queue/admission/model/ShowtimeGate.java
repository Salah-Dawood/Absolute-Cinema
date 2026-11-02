package com.cinemasaar.queue.admission.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "showtime_gates")
public class ShowtimeGate {

    @Id
    private String showtimeId;
    private int capacity;
    private int activeSessions;
    private Instant updatedAt;

    public ShowtimeGate() {
    }

    public ShowtimeGate(String showtimeId, int capacity, int activeSessions, Instant updatedAt) {
        this.showtimeId = showtimeId;
        this.capacity = capacity;
        this.activeSessions = activeSessions;
        this.updatedAt = updatedAt;
    }

    public String getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(String showtimeId) {
        this.showtimeId = showtimeId;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(int activeSessions) {
        this.activeSessions = activeSessions;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
