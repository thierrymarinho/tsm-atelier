package com.tm.tsm_atelier.domain.order.dto;

import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.entity.ShippingAddress;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDTO(Long id, OrderStatus status, BigDecimal totalAmount, BigDecimal shippingFee,
		String clientSecret, // Returned only upon checkout for Stripe
		ShippingAddress shippingAddress, LocalDateTime expiresAt, LocalDateTime createdAt,
		List<OrderItemResponseDTO> items) {
}
