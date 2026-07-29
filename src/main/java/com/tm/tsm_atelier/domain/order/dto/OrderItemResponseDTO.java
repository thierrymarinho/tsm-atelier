package com.tm.tsm_atelier.domain.order.dto;

import java.math.BigDecimal;

public record OrderItemResponseDTO(Long id, Long skuId, String productName, String skuCode, String size, String color,
		String imageUrl, BigDecimal priceAtPurchase, Integer quantity) {
}
