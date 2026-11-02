package com.cinemasaar.queue.payment.service;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentResult charge(String intentId, BigDecimal amount, String method);
}
