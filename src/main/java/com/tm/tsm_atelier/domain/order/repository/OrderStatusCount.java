package com.tm.tsm_atelier.domain.order.repository;

import com.tm.tsm_atelier.domain.order.entity.OrderStatus;

/**
 * Projeção do {@code GROUP BY status}. Existe para o agrupamento voltar tipado
 * em vez de {@code Object[]}, onde a ordem das colunas vira convenção não
 * verificada.
 */
public record OrderStatusCount(OrderStatus status, long total) {
}
