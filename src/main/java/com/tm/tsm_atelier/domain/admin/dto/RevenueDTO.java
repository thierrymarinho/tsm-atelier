package com.tm.tsm_atelier.domain.admin.dto;

import java.math.BigDecimal;

/**
 * Janelas fixas, e não um intervalo parametrizado: a home de um painel serve
 * para bater o olho, e quem precisa recortar um período tem os filtros de GET
 * /api/v1/admin/orders. Duas telas com seletor de data seriam duas fontes de
 * verdade para a mesma pergunta.
 *
 *
 * É valor de pedido, e não dinheiro liquidado. Só entram pedidos em PAID,
 * SHIPPED e DELIVERED — PENDING_PAYMENT ainda não é dinheiro, PAYMENT_FAILED
 * nunca foi, e CANCELLED sai da conta. Só que cancelar um pedido pago não
 * estorna nada hoje, então um cancelamento desses tira o valor daqui sem tirar
 * o dinheiro da Stripe. Enquanto o estorno não existir, este número é o que a
 * loja deve reconhecer, e não o que ela recebeu.
 */
public record RevenueDTO(BigDecimal today, BigDecimal last7Days, BigDecimal last30Days) {
}
