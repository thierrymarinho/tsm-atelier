package com.tm.tsm_atelier.domain.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductSummaryDTO(Long id, String name, String slug, BigDecimal price, boolean featured,
		String coverImageUrl, String hoverImageUrl, List<String> colorsHex) {
}
