package com.cinemasaar.queue.payment.service;

public record PaymentResult(boolean success, String reference, String failureReason) {
}
