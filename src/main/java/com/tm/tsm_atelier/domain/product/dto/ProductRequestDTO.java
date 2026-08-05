package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequestDTO(
		@NotBlank(message = "Product name is required") @Size(max = 255, message = "Product name cannot exceed 255 characters") String name,

		String description,

		@Valid List<FabricCompositionRequestDTO> fabricCompositions,

		List<@Size(max = 255, message = "Care instruction cannot exceed 255 characters") String> careInstructions,

		@NotNull(message = "Price is required") @Positive(message = "Price must be greater than zero") BigDecimal price,

		/**
		 * Opcional. Ausente ou nulo retira o produto da promocao: o update e um PUT, de
		 * substituicao total, e trata campo ausente como remocao — mesma semantica ja
		 * usada por collectionId.
		 */
		@Positive(message = "Promotional price must be greater than zero") BigDecimal promotionalPrice,

		Long collectionId,

		@NotNull(message = "Category is required") Category category,

		@NotNull(message = "Target audience is required") TargetAudience targetAudience,

		boolean active,

		boolean featured,

		@NotEmpty(message = "Product must have at least one color") @Valid List<ProductColorRequestDTO> colors) {
}
