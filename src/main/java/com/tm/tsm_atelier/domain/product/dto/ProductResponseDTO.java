package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;
import java.util.List;

public record ProductResponseDTO(Long id, String name, String slug, String description,
		List<FabricCompositionResponseDTO> fabricCompositions, List<String> careInstructions, BigDecimal price,
		CollectionResponseDTO collection, Category category, TargetAudience targetAudience, boolean active,
		boolean featured, List<ProductColorResponseDTO> colors) {
}
