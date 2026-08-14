package com.tm.tsm_atelier.domain.product.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * O card da vitrine. Este é o payload mais servido e mais cacheado da
 * aplicação, então tudo o que entra aqui é multiplicado por página, por filtro
 * e por entrada no Redis.
 */
public record ProductSummaryDTO(Long id, String name, String slug, BigDecimal price, BigDecimal promotionalPrice,
		boolean featured, String coverImageUrl, String hoverImageUrl, List<String> colorsHex, boolean active) {
}
