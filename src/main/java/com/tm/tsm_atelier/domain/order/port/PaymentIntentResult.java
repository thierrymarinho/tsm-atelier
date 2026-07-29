package com.tm.tsm_atelier.domain.order.port;

public record PaymentIntentResult(String paymentIntentId, String clientSecret) {
}
