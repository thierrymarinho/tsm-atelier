package com.tm.tsm_atelier.common.exception.custom;

import com.tm.tsm_atelier.domain.order.entity.OrderStatus;

/**
 * Transição de status de pedido que não existe no fluxo. Tipo próprio, e não
 * uma {@link BusinessRuleException} genérica, para que o front consiga
 * distinguir "esse caminho não existe" de "esse dado está inválido" sem
 * depender do texto da mensagem.
 */
public class InvalidStatusTransitionException extends BusinessRuleException {

	private final OrderStatus from;
	private final OrderStatus to;

	public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
		super("Cannot move an order from " + from + " to " + to + ".");
		this.from = from;
		this.to = to;
	}

	public OrderStatus getFrom() {
		return from;
	}

	public OrderStatus getTo() {
		return to;
	}
}
