package com.cinemasaar.queue.payment.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(String intentId, BigDecimal amount, String method) {
        return new PaymentResult(
                true,
                "SIM-" + UUID.randomUUID().toString().substring(0, 8),
                null);
    }
}
