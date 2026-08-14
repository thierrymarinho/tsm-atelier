package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import java.time.LocalDateTime;

public record AdminProductSKUResponseDTO(Long id, Long version, ProductSize size, String skuCode, Integer stockQuantity,
		LocalDateTime deletedAt) {
}
