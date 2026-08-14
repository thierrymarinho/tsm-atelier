package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminProductResponseDTO(Long id, String name, String slug, String description,
		List<FabricCompositionResponseDTO> fabricCompositions, List<CareInstructionResponseDTO> careInstructions,
		BigDecimal price, BigDecimal promotionalPrice, CollectionResponseDTO collection, Category category,
		TargetAudience targetAudience, boolean active, boolean featured, List<AdminProductColorResponseDTO> colors,
		LocalDateTime deletedAt) {
}
