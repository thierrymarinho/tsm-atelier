package com.tm.tsm_atelier.domain.cart.dto;

import java.math.BigDecimal;

public record CartItemResponseDTO(Long id, Long skuId, String skuCode, String size, Long productId, String productName,
		String productSlug, String colorName, String coverImageUrl, Integer quantity, BigDecimal unitPrice,
		BigDecimal subtotal, Integer stockQuantity, Boolean available) {
}
