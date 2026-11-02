package com.cinemasaar.queue.payment.controller;

import com.cinemasaar.queue.common.TransientTransactionRetrier;
import com.cinemasaar.queue.payment.dto.PayRequest;
import com.cinemasaar.queue.payment.dto.PaymentResponse;
import com.cinemasaar.queue.payment.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final TransientTransactionRetrier retrier;

    public PaymentController(PaymentService paymentService,
                             TransientTransactionRetrier retrier) {
        this.paymentService = paymentService;
        this.retrier = retrier;
    }

    @PostMapping("/{intentId}/pay")
    public PaymentResponse pay(@PathVariable String intentId,
                               @RequestHeader("X-User-Id") String userId,
                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                               @RequestBody(required = false) PayRequest request) {
        String method = request == null || request.method() == null
                ? "SIMULATED"
                : request.method();
        return retrier.execute(
                () -> paymentService.pay(intentId, userId, idempotencyKey, method));
    }

    @GetMapping("/{intentId}")
    public PaymentResponse status(@PathVariable String intentId,
                                  @RequestHeader("X-User-Id") String userId) {
        return paymentService.get(intentId, userId);
    }
}
