package com.tm.tsm_atelier.domain.product.dto;

public record StockResponseDTO(Long skuId, String skuCode, Integer stockQuantity, Long version) {
}
