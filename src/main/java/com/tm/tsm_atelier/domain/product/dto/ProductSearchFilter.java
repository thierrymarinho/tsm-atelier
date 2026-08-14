package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;

public record ProductSearchFilter(String searchTerm, Category category, TargetAudience targetAudience,
		Long collectionId, BigDecimal minPrice, BigDecimal maxPrice, Boolean isFeatured, Boolean onSale) {
}
