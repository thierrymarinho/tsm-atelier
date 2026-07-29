package com.tm.tsm_atelier.domain.order.port;

import com.tm.tsm_atelier.domain.order.entity.Order;

public interface PaymentGatewayPort {
	PaymentIntentResult createPaymentIntent(Order order);
}
