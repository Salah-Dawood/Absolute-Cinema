package com.cinemasaar.queue.admission.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "queue_entries")
@CompoundIndex(name = "uq_showtime_user", def = "{'showtimeId': 1, 'userId': 1}", unique = true)
@CompoundIndex(name = "uq_admission_token", def = "{'admissionToken': 1}", unique = true, sparse = true)
@CompoundIndex(name = "idx_showtime_status_joined", def = "{'showtimeId': 1, 'status': 1, 'joinedAt': 1}")
public class QueueEntry {

    @Id
    private String id;
    private String showtimeId;
    private String userId;
    private QueueStatus status;
    private String admissionToken;
    private Instant joinedAt;
    private Instant admittedAt;
    private Instant admissionExpiresAt;
    private String cartId;

    public static QueueEntry waiting(String showtimeId, String userId, Instant now) {
        QueueEntry entry = new QueueEntry();
        entry.showtimeId = showtimeId;
        entry.userId = userId;
        entry.status = QueueStatus.WAITING;
        entry.joinedAt = now;
        return entry;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(String showtimeId) {
        this.showtimeId = showtimeId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public String getAdmissionToken() {
        return admissionToken;
    }

    public void setAdmissionToken(String admissionToken) {
        this.admissionToken = admissionToken;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getAdmittedAt() {
        return admittedAt;
    }

    public void setAdmittedAt(Instant admittedAt) {
        this.admittedAt = admittedAt;
    }

    public Instant getAdmissionExpiresAt() {
        return admissionExpiresAt;
    }

    public void setAdmissionExpiresAt(Instant admissionExpiresAt) {
        this.admissionExpiresAt = admissionExpiresAt;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }
}
