package com.tm.tsm_atelier.domain.order.listener;

import com.tm.tsm_atelier.domain.common.port.EmailPort;
import com.tm.tsm_atelier.domain.order.event.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderConfirmationEmailListener {

	private static final Logger log = LoggerFactory.getLogger(OrderConfirmationEmailListener.class);

	private final EmailPort emailPort;

	public OrderConfirmationEmailListener(EmailPort emailPort) {
		this.emailPort = emailPort;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderPaid(OrderPaidEvent event) {
		try {
			emailPort.sendOrderConfirmationEmail(event.customerEmail(), event.customerFirstName(), event.orderId(),
					event.totalAmount());
		} catch (Exception e) {
			log.error("Failed to send order confirmation e-mail for order {}", event.orderId(), e);
		}
	}
}
