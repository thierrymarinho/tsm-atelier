package com.tm.tsm_atelier.domain.order.dto;

import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.entity.ShippingAddress;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A visão do admin sobre um pedido. Separada de {@link OrderResponseDTO} por
 * dois motivos, e os dois vinham do mesmo record servir os dois públicos.
 *
 * <p>
 * <strong>O que entrou:</strong> quem é o cliente. A listagem do admin não
 * trazia nome, e-mail nem id do comprador — o mais perto disso era o endereço
 * de entrega —, então não havia como atender "cadê meu pedido?" sem ir ao
 * banco.
 *
 * <p>
 * <strong>O que saiu:</strong> o {@code clientSecret}. Ele voltava preenchido
 * para todo pedido pendente, e a listagem do admin carregava o segredo de
 * pagamento de todos os clientes de uma vez. É a credencial que o navegador usa
 * para confirmar o PaymentIntent na Stripe; o painel não faz nada com ela, e o
 * valor acabava em log de proxy, cache e devtools sem contrapartida nenhuma.
 */
public record AdminOrderResponseDTO(Long id, OrderStatus status, BigDecimal totalAmount, BigDecimal shippingFee,
		UUID customerId, String customerName, String customerEmail, ShippingAddress shippingAddress,
		LocalDateTime expiresAt, LocalDateTime createdAt, List<OrderItemResponseDTO> items) {
}
