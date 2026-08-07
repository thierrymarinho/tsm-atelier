package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequestDTO(
		@NotBlank(message = "Product name is required") @Size(max = 255, message = "Product name cannot exceed 255 characters") String name,

		/**
		 * A coluna e TEXT, sem limite. O limite aqui existe para o corpo da requisicao
		 * ter um teto conhecido, nao para caber na coluna.
		 */
		@Size(max = 5000, message = "Description cannot exceed 5000 characters") String description,

		@Valid List<FabricCompositionRequestDTO> fabricCompositions,

		List<@Size(max = 255, message = "Care instruction cannot exceed 255 characters") String> careInstructions,

		/**
		 * O @Digits espelha a coluna DECIMAL(10, 2). Sem ele, um valor com mais de duas
		 * casas era arredondado em silencio pelo banco — 29.999 virava 30.00 — e um
		 * valor acima de 99.999.999,99 voltava como 409 "A data conflict occurred", que
		 * nao diz nada sobre o preco.
		 */
		@NotNull(message = "Price is required") @Positive(message = "Price must be greater than zero") @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places") BigDecimal price,

		/**
		 * Opcional. Ausente ou nulo retira o produto da promocao: o update e um PUT, de
		 * substituicao total, e trata campo ausente como remocao — mesma semantica ja
		 * usada por collectionId.
		 */
		@Positive(message = "Promotional price must be greater than zero") @Digits(integer = 8, fraction = 2, message = "Promotional price must have at most 8 integer digits and 2 decimal places") BigDecimal promotionalPrice,

		Long collectionId,

		@NotNull(message = "Category is required") Category category,

		@NotNull(message = "Target audience is required") TargetAudience targetAudience,

		boolean active,

		boolean featured,

		@NotEmpty(message = "Product must have at least one color") @Valid List<ProductColorRequestDTO> colors) {
}
