package com.tm.tsm_atelier.domain.order.event;

import java.math.BigDecimal;

/**
 * Published when an order transitions to PAID. Consumers run after the
 * transaction commits, so a failing side effect (e.g. e-mail delivery) can
 * never roll back a confirmed payment.
 */
public record OrderPaidEvent(Long orderId, String customerEmail, String customerFirstName, BigDecimal totalAmount) {
}
