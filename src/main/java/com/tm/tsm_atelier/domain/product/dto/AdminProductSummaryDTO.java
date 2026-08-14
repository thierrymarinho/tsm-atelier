package com.tm.tsm_atelier.domain.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * O card da busca do admin. O {@code deletedAt} é o que a listagem usa para
 * marcar o produto como removido e oferecer a restauração — a busca do admin é
 * a única que enxerga esses registros.
 */
public record AdminProductSummaryDTO(Long id, String name, String slug, BigDecimal price, BigDecimal promotionalPrice,
		boolean featured, String coverImageUrl, String hoverImageUrl, List<String> colorsHex, LocalDateTime deletedAt,
		boolean active) {
}
