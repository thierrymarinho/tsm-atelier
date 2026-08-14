package com.tm.tsm_atelier.domain.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminProductSummaryDTO(Long id, String name, String slug, BigDecimal price, BigDecimal promotionalPrice,
		boolean featured, String coverImageUrl, String hoverImageUrl, List<String> colorsHex, LocalDateTime deletedAt,
		boolean active) {
}
