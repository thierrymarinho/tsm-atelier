package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.ProductSize;

public record ProductSKUResponseDTO(Long id, ProductSize size, String skuCode, Integer stockQuantity,
		java.time.LocalDateTime deletedAt) {
}
