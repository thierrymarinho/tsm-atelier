package com.tm.tsm_atelier.domain.collection.dto;

import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CollectionRequestDTO(
		@NotBlank(message = "Collection name is required") @Size(max = 255, message = "Collection name cannot exceed 255 characters") String name,

		boolean active, String description,
		@Size(max = 255, message = "Hero image URL cannot exceed 255 characters") String heroImageUrl,
		@Size(max = 255, message = "Portrait image URL cannot exceed 255 characters") String portraitImageUrl,
		@Size(max = 255, message = "Square image URL cannot exceed 255 characters") String squareImageUrl,
		DisplayPosition displayPosition, Integer displayOrder,
		@NotNull(message = "Target audience is required") TargetAudience targetAudience) {
}
