package com.tm.tsm_atelier.domain.order.dto;

import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.entity.ShippingAddress;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminOrderResponseDTO(Long id, OrderStatus status, BigDecimal totalAmount, BigDecimal shippingFee,
		UUID customerId, String customerName, String customerEmail, ShippingAddress shippingAddress,
		LocalDateTime expiresAt, LocalDateTime createdAt, List<OrderItemResponseDTO> items) {
}
