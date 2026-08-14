package com.tm.tsm_atelier.common.exception.custom;

import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import lombok.Getter;

/**
 * Transição de status de pedido que não existe no fluxo. Tipo próprio, e não
 * uma BusinessRuleException genérica, para que o front consiga distinguir "esse
 * caminho não existe" de "esse dado está inválido" sem depender do texto da
 * mensagem.
 */
@Getter
public class InvalidStatusTransitionException extends BusinessRuleException {

	private final OrderStatus from;
	private final OrderStatus to;

	public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
		super("Cannot move an order from " + from + " to " + to + ".");
		this.from = from;
		this.to = to;
	}

}
