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
 * Orquestra o checkout em duas transações curtas, com a chamada ao gateway de
 * pagamento entre elas.
 *
 * Esta classe não é transacional, e isso é deliberado. Falar com a Stripe
 * segurando os locks pessimistas tomados na reserva de estoque serializaria
 * todo checkout do mesmo SKU atrás da latência do gateway, prenderia uma
 * conexão do pool por esse tempo e, num rollback depois da chamada, deixaria um
 * PaymentIntent órfão que nenhum pedido consegue casar.
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
	 * Devolve o estoque na hora. O pedido já está commitado, então uma falha do
	 * gateway deixaria as unidades reservadas até o scheduler expirá-lo.
	 */
	private void releaseReservation(Long orderId) {
		try {
			orderService.cancelAndRestoreStock(orderId);
		} catch (RuntimeException e) {
			log.error("Failed to release stock for order {} after payment gateway error", orderId, e);
		}
	}
}
