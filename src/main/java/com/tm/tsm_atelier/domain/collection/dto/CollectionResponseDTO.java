package com.tm.tsm_atelier.domain.collection.dto;

import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;

public record CollectionResponseDTO(Long id, String name, String slug, String description, boolean active,
		String heroImageUrl, String portraitImageUrl, String squareImageUrl, DisplayPosition displayPosition,
		Integer displayOrder, TargetAudience targetAudience) {
}
