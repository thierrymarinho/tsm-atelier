package com.tm.tsm_atelier.domain.order.service;

import com.tm.tsm_atelier.domain.order.dto.CheckoutRequestDTO;
import com.tm.tsm_atelier.domain.order.dto.OrderResponseDTO;
import com.tm.tsm_atelier.domain.order.entity.Order;
import com.tm.tsm_atelier.domain.order.port.PaymentGatewayPort;
import com.tm.tsm_atelier.domain.order.port.PaymentIntentResult;
import com.tm.tsm_atelier.domain.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates checkout across two short transactions with the payment gateway
 * call in between.
 *
 * <p>
 * This class is intentionally <b>not</b> transactional: talking to Stripe while
 * holding the pessimistic SKU locks taken during stock reservation would
 * serialize every checkout of the same SKU behind the gateway's latency, tie up
 * a pool connection for the duration, and — on a rollback after the gateway
 * call — leave an orphan PaymentIntent that no order can be matched to.
 */
@Service
public class CheckoutService {

	private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

	private final OrderService orderService;
	private final PaymentGatewayPort paymentGatewayPort;

	public CheckoutService(OrderService orderService, PaymentGatewayPort paymentGatewayPort) {
		this.orderService = orderService;
		this.paymentGatewayPort = paymentGatewayPort;
	}

	public OrderResponseDTO checkout(User user, CheckoutRequestDTO request) {
		Order order = orderService.createPendingOrder(user, request);

		PaymentIntentResult paymentIntentResult;
		try {
			paymentIntentResult = paymentGatewayPort.createPaymentIntent(order);
		} catch (RuntimeException e) {
			releaseReservation(order.getId());
			throw e;
		}

		return orderService.attachPaymentIntent(order.getId(), paymentIntentResult);
	}

	/**
	 * The order is already committed, so a gateway failure would otherwise keep its
	 * stock reserved until the scheduler expires it. Release it right away.
	 */
	private void releaseReservation(Long orderId) {
		try {
			orderService.cancelAndRestoreStock(orderId);
		} catch (RuntimeException e) {
			// Never mask the original gateway failure; the expiration scheduler is the
			// backstop for this order.
			log.error("Failed to release stock for order {} after payment gateway error", orderId, e);
		}
	}
}
