package com.tm.tsm_atelier.domain.collection.dto;

import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import jakarta.validation.constraints.NotBlank;

public record CollectionRequestDTO(@NotBlank(message = "Collection name is required") String name,

		boolean active, String description, String heroImageUrl, String portraitImageUrl, String squareImageUrl,
		DisplayPosition displayPosition, Integer displayOrder, TargetAudience targetAudience) {
}
