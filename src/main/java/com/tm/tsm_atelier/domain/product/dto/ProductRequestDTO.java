package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequestDTO(@NotBlank(message = "Product name is required") String name,

		String description,

		@Valid List<FabricCompositionRequestDTO> fabricCompositions,

		List<String> careInstructions,

		@NotNull(message = "Price is required") @Positive(message = "Price must be greater than zero") BigDecimal price,

		Long collectionId,

		@NotNull(message = "Category is required") Category category,

		@NotNull(message = "Target audience is required") TargetAudience targetAudience,

		boolean active,

		boolean featured,

		@NotEmpty(message = "Product must have at least one color") @Valid List<ProductColorRequestDTO> colors) {
}
