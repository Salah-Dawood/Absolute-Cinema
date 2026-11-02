package com.cinemasaar.queue.admission.service;

import com.cinemasaar.queue.admission.dto.QueueStatsResponse;
import com.cinemasaar.queue.admission.dto.QueueStatusResponse;
import com.cinemasaar.queue.admission.exception.AdmissionExpiredException;
import com.cinemasaar.queue.admission.exception.AdmissionRequiredException;
import com.cinemasaar.queue.admission.model.QueueEntry;
import com.cinemasaar.queue.admission.model.QueueStatus;
import com.cinemasaar.queue.admission.model.ShowtimeGate;
import com.cinemasaar.queue.admission.repository.QueueEntryRepository;
import com.cinemasaar.queue.admission.repository.ShowtimeGateRepository;
import com.cinemasaar.queue.common.exception.NotFoundException;
import com.cinemasaar.queue.config.BookingProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class AdmissionService {

    private final MongoTemplate mongo;
    private final QueueEntryRepository entries;
    private final ShowtimeGateRepository gates;
    private final BookingProperties props;

    public AdmissionService(MongoTemplate mongo,
                            QueueEntryRepository entries,
                            ShowtimeGateRepository gates,
                            BookingProperties props) {
        this.mongo = mongo;
        this.entries = entries;
        this.gates = gates;
        this.props = props;
    }

    public QueueStatusResponse join(String showtimeId, String userId) {
        Instant now = Instant.now();
        QueueEntry entry = entries.findByShowtimeIdAndUserId(showtimeId, userId).orElse(null);

        if (entry == null) {
            try {
                entry = entries.insert(QueueEntry.waiting(showtimeId, userId, now));
            } catch (DuplicateKeyException race) {
                entry = entries.findByShowtimeIdAndUserId(showtimeId, userId).orElseThrow();
            }
            return respond(entry);
        }

        // A token that expired before the worker noticed should not be handed back.
        if (entry.getStatus() == QueueStatus.ADMITTED
                && entry.getCartId() == null
                && entry.getAdmissionExpiresAt() != null
                && !entry.getAdmissionExpiresAt().isAfter(now)) {
            if (markExpired(entry.getId(), now)) {
                release(entry.getShowtimeId());
            }
            entry = entries.findById(entry.getId()).orElse(entry);
        }

        if (entry.getStatus() == QueueStatus.WAITING
                || entry.getStatus() == QueueStatus.ADMITTED) {
            return respond(entry);
        }

        Query q = Query.query(Criteria.where("_id").is(entry.getId())
                .and("status").in(QueueStatus.EXPIRED, QueueStatus.COMPLETED, QueueStatus.CANCELLED));
        Update u = new Update()
                .set("status", QueueStatus.WAITING)
                .set("joinedAt", now)
                .unset("admissionToken")
                .unset("admittedAt")
                .unset("admissionExpiresAt")
                .unset("cartId");

        QueueEntry rejoined = mongo.findAndModify(q, u,
                FindAndModifyOptions.options().returnNew(true), QueueEntry.class);

        return respond(rejoined != null
                ? rejoined
                : entries.findByShowtimeIdAndUserId(showtimeId, userId).orElseThrow());
    }

    public QueueStatusResponse status(String showtimeId, String userId) {
        QueueEntry entry = entries.findByShowtimeIdAndUserId(showtimeId, userId)
                .orElseThrow(() -> new NotFoundException("No queue entry for this user and showtime"));

        if (entry.getStatus() == QueueStatus.ADMITTED
                && entry.getCartId() == null
                && entry.getAdmissionExpiresAt() != null
                && !entry.getAdmissionExpiresAt().isAfter(Instant.now())) {
            Instant now = Instant.now();
            if (markExpired(entry.getId(), now)) {
                release(entry.getShowtimeId());
            }
            entry = entries.findById(entry.getId()).orElse(entry);
        }
        return respond(entry);
    }

    public QueueStatsResponse stats(String showtimeId) {
        long waiting = entries.countByShowtimeIdAndStatus(showtimeId, QueueStatus.WAITING);
        ShowtimeGate gate = gates.findById(showtimeId).orElse(null);
        return new QueueStatsResponse(
                showtimeId,
                waiting,
                gate == null ? 0 : gate.getActiveSessions(),
                gate == null ? props.maxActiveSessionsPerShowtime() : gate.getCapacity());
    }

    public List<String> showtimesWithWaitingUsers() {
        return mongo.findDistinct(
                Query.query(Criteria.where("status").is(QueueStatus.WAITING)),
                "showtimeId", QueueEntry.class, String.class);
    }

    public boolean tryAdmitNext(String showtimeId) {
        if (!tryAcquire(showtimeId)) {
            return false;
        }

        Instant now = Instant.now();
        Query q = Query.query(Criteria.where("showtimeId").is(showtimeId)
                        .and("status").is(QueueStatus.WAITING))
                .with(Sort.by(Sort.Direction.ASC, "joinedAt"));

        Update u = new Update()
                .set("status", QueueStatus.ADMITTED)
                .set("admittedAt", now)
                .set("admissionToken", UUID.randomUUID().toString())
                .set("admissionExpiresAt", now.plus(props.admissionTtl()));

        QueueEntry admitted = mongo.findAndModify(q, u,
                FindAndModifyOptions.options().returnNew(true), QueueEntry.class);

        if (admitted == null) {
            release(showtimeId);
            return false;
        }
        return true;
    }

    public int expireAdmissions() {
        Instant now = Instant.now();
        List<QueueEntry> expired = entries
                .findTop100ByStatusAndCartIdIsNullAndAdmissionExpiresAtLessThanEqualOrderByAdmissionExpiresAtAsc(
                        QueueStatus.ADMITTED, now);

        int released = 0;
        for (QueueEntry entry : expired) {
            if (markExpired(entry.getId(), now)) {
                release(entry.getShowtimeId());
                released++;
            }
        }
        return released;
    }

    /** Validates a single-use admission token before a cart is created. */
    public QueueEntry requireAdmission(String token, String userId) {
        QueueEntry entry = entries.findByAdmissionToken(token)
                .orElseThrow(AdmissionRequiredException::new);

        if (entry.getStatus() != QueueStatus.ADMITTED) {
            throw new AdmissionExpiredException();
        }
        if (!entry.getUserId().equals(userId)) {
            throw new AdmissionRequiredException("Admission token belongs to another user");
        }
        if (entry.getCartId() != null) {
            throw new AdmissionRequiredException("Admission token already used");
        }
        if (entry.getAdmissionExpiresAt() == null
                || !entry.getAdmissionExpiresAt().isAfter(Instant.now())) {
            throw new AdmissionExpiredException();
        }
        return entry;
    }

    /** Binds a cart to the admission entry; the token becomes single-use. */
    public void attachCart(String queueEntryId, String cartId, Instant cartExpiresAt) {
        Instant now = Instant.now();
        // CAS: only an admitted entry that is still unused and unexpired may
        // bind a cart. Two concurrent cart creations cannot both win.
        Criteria c = Criteria.where("_id").is(queueEntryId)
                .and("status").is(QueueStatus.ADMITTED)
                .and("cartId").is(null)
                .and("admissionExpiresAt").gt(now);

        Update u = new Update()
                .set("cartId", cartId)
                .set("admissionExpiresAt", cartExpiresAt);

        if (mongo.updateFirst(Query.query(c), u, QueueEntry.class).getModifiedCount() != 1) {
            throw new AdmissionExpiredException();
        }
    }

    /** Idempotent: releases the gate only for the transition that actually wins. */
    public void completeByCart(String cartId) {
        QueueEntry entry = entries.findByCartId(cartId).orElse(null);
        if (entry == null) {
            return;
        }

        Query q = Query.query(Criteria.where("_id").is(entry.getId())
                .and("status").is(QueueStatus.ADMITTED));
        Update u = new Update()
                .set("status", QueueStatus.COMPLETED)
                .unset("admissionToken");

        if (mongo.updateFirst(q, u, QueueEntry.class).getModifiedCount() == 1) {
            release(entry.getShowtimeId());
        }
    }

    private boolean tryAcquire(String showtimeId) {
        Query q = Query.query(Criteria.where("_id").is(showtimeId)
                .and("activeSessions").lt(props.maxActiveSessionsPerShowtime()));
        Update u = new Update()
                .inc("activeSessions", 1)
                .set("updatedAt", Instant.now());

        ShowtimeGate gate = mongo.findAndModify(q, u,
                FindAndModifyOptions.options().returnNew(true), ShowtimeGate.class);
        if (gate != null) {
            return true;
        }
        if (gates.existsById(showtimeId)) {
            return false;   // gate exists and is full
        }

        try {
            mongo.insert(new ShowtimeGate(showtimeId,
                    props.maxActiveSessionsPerShowtime(), 1, Instant.now()));
            return true;
        } catch (DuplicateKeyException race) {
            return mongo.findAndModify(q, u,
                    FindAndModifyOptions.options().returnNew(true), ShowtimeGate.class) != null;
        }
    }

    public void release(String showtimeId) {
        mongo.updateFirst(
                Query.query(Criteria.where("_id").is(showtimeId).and("activeSessions").gt(0)),
                new Update().inc("activeSessions", -1).set("updatedAt", Instant.now()),
                ShowtimeGate.class);
    }

    private boolean markExpired(String queueEntryId, Instant now) {
        Query q = Query.query(Criteria.where("_id").is(queueEntryId)
                .and("status").is(QueueStatus.ADMITTED)
                .and("cartId").is(null)
                .and("admissionExpiresAt").lte(now));
        Update u = new Update()
                .set("status", QueueStatus.EXPIRED)
                .unset("admissionToken");
        return mongo.updateFirst(q, u, QueueEntry.class).getModifiedCount() == 1;
    }

    private QueueStatusResponse respond(QueueEntry entry) {
        long position = 0;
        if (entry.getStatus() == QueueStatus.WAITING) {
            position = entries.countByShowtimeIdAndStatusAndJoinedAtLessThan(
                    entry.getShowtimeId(), QueueStatus.WAITING, entry.getJoinedAt()) + 1;
        }

        ShowtimeGate gate = gates.findById(entry.getShowtimeId()).orElse(null);
        return new QueueStatusResponse(
                entry.getId(),
                entry.getStatus(),
                position,
                gate == null ? 0 : gate.getActiveSessions(),
                gate == null ? props.maxActiveSessionsPerShowtime() : gate.getCapacity(),
                entry.getStatus() == QueueStatus.ADMITTED ? entry.getAdmissionToken() : null,
                entry.getAdmissionExpiresAt(),
                entry.getCartId(),
                entry.getJoinedAt());
    }
}
