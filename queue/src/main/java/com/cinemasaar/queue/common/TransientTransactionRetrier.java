package com.cinemasaar.queue.common;

import com.mongodb.MongoException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Replays an operation when MongoDB aborts its transaction with a transient
 * error (e.g. a write conflict between two concurrent seat clicks).
 * MongoDB marks these errors as safe to retry with the
 * "TransientTransactionError" label; anything else is rethrown immediately.
 */
@Component
public class TransientTransactionRetrier {

    private static final Logger log = LoggerFactory.getLogger(TransientTransactionRetrier.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 20;
    private static final int WRITE_CONFLICT_CODE = 112;

    public <T> T execute(Supplier<T> operation) {
        for (int attempt = 1; ; attempt++) {
            try {
                return operation.get();
            } catch (RuntimeException e) {
                if (attempt >= MAX_ATTEMPTS || !isTransient(e)) {
                    throw e;
                }
                log.debug("Transient transaction error, retrying (attempt {}/{})",
                        attempt + 1, MAX_ATTEMPTS);
                backoff(attempt);
            }
        }
    }

    private boolean isTransient(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof MongoException mongo
                    && (mongo.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)
                        || mongo.getCode() == WRITE_CONFLICT_CODE)) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(BASE_BACKOFF_MS * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying", ie);
        }
    }
}
