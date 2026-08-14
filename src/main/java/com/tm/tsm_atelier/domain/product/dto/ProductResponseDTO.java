package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;
import java.util.List;

/**
 * O produto como a loja o vê. Sem {@code deletedAt}: o catálogo só devolve
 * produto vivo, e cores e SKUs removidos são filtrados no mapeamento, então o
 * campo era constantemente nulo em toda a árvore.
 *
 * <p>
 * O {@code active} fica, e não por descuido: {@code findBySlug} filtra apenas
 * {@code deletedAt}, então um produto tirado da vitrine continua acessível pela
 * URL direta. Enquanto for assim, esconder a flag do cliente seria pior do que
 * mostrá-la.
 */
public record ProductResponseDTO(Long id, String name, String slug, String description,
		List<FabricCompositionResponseDTO> fabricCompositions, List<CareInstructionResponseDTO> careInstructions,
		BigDecimal price, BigDecimal promotionalPrice, CollectionResponseDTO collection, Category category,
		TargetAudience targetAudience, boolean active, boolean featured, List<ProductColorResponseDTO> colors) {
}
