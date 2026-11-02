package com.cinemasaar.queue.reservation.service;

import com.cinemasaar.queue.common.TransientTransactionRetrier;
import com.cinemasaar.queue.config.BookingProperties;
import com.cinemasaar.queue.reservation.exception.BookingBusyException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class SeatOperationExecutor {

    private final ThreadPoolTaskExecutor executor;
    private final TransientTransactionRetrier retrier;
    private final Duration timeout;

    public SeatOperationExecutor(
            @Qualifier("seatOperationPool") ThreadPoolTaskExecutor executor,
            TransientTransactionRetrier retrier,
            BookingProperties props) {
        this.executor = executor;
        this.retrier = retrier;
        this.timeout = props.seatOperationTimeout();
    }

    public <T> T submit(Callable<T> task) {
        // A Mongo write conflict aborts the transaction, so replaying the whole
        // operation is safe and turns a spurious 409 into the real outcome.
        return retrier.execute(() -> submitOnce(task));
    }

    private <T> T submitOnce(Callable<T> task) {
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new BookingBusyException("Seat operation timed out; retry");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new BookingBusyException("Seat operation failed", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BookingBusyException("Interrupted while waiting for a seat operation", e);
        }
    }
}
