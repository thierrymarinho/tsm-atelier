package com.tm.tsm_atelier.domain.order.event;

import java.math.BigDecimal;

/**
 * Publicado quando um pedido passa para PAID. Os consumidores rodam depois do
 * commit da transação, então um efeito colateral que falhe — o envio do e-mail,
 * por exemplo — nunca desfaz um pagamento já confirmado.
 */
public record OrderPaidEvent(Long orderId, String customerEmail, String customerFirstName, BigDecimal totalAmount) {
}
