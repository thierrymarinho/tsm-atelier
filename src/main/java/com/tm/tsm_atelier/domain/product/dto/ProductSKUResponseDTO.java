package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.ProductSize;

/**
 * O SKU como a loja o vê: tamanho, código e disponibilidade. Nada de
 * {@code version} ou {@code deletedAt} — ver
 * {@link AdminProductSKUResponseDTO}.
 */
public record ProductSKUResponseDTO(Long id, ProductSize size, String skuCode, Integer stockQuantity) {
}
