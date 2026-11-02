package com.cinemasaar.queue.payment.service;

import com.cinemasaar.queue.common.exception.NotFoundException;
import com.cinemasaar.queue.payment.dto.PaymentResponse;
import com.cinemasaar.queue.payment.exception.PaymentException;
import com.cinemasaar.queue.payment.model.PaymentIntent;
import com.cinemasaar.queue.payment.model.PaymentStatus;
import com.cinemasaar.queue.payment.repository.PaymentIntentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentTransactionService tx;
    private final PaymentGateway gateway;
    private final PaymentIntentRepository payments;

    public PaymentService(PaymentTransactionService tx,
                          PaymentGateway gateway,
                          PaymentIntentRepository payments) {
        this.tx = tx;
        this.gateway = gateway;
        this.payments = payments;
    }

    /**
     * Deliberately not transactional: the gateway call sits between two short
     * database transactions, so a slow payment never holds seat locks.
     */
    public PaymentResponse pay(String intentId, String userId,
                               String idempotencyKey, String method) {
        PaymentIntent claimed = tx.claim(intentId, userId, idempotencyKey, method);

        if (claimed.getStatus() == PaymentStatus.PAID) {
            return PaymentResponse.from(claimed);
        }

        PaymentResult result = gateway.charge(claimed.getId(), claimed.getAmount(), method);

        if (!result.success()) {
            tx.fail(claimed.getId(), result.failureReason());
            throw new PaymentException("PAYMENT_FAILED",
                    result.failureReason() == null ? "Payment declined" : result.failureReason());
        }

        try {
            return PaymentResponse.from(tx.confirm(claimed.getId(), result));
        } catch (PaymentException e) {
            // The gateway may have moved money after the cart was released.
            tx.refundRequired(claimed.getId(), e.getMessage());
            throw e;
        }
    }

    public PaymentResponse get(String intentId, String userId) {
        return PaymentResponse.from(payments.findByIdAndUserId(intentId, userId)
                .orElseThrow(() -> new NotFoundException("Payment not found")));
    }
}
