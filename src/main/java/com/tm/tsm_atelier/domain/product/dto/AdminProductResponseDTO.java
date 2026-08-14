package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * O produto como o painel o vê. Separado de {@link ProductResponseDTO} pelo
 * mesmo motivo de {@code AdminOrderResponseDTO}: um record servindo dois
 * públicos publica na vitrine tudo o que o admin vier a precisar.
 *
 * <p>
 * O que este tipo tem a mais é escrituração — {@code deletedAt} no produto, nas
 * cores e nos SKUs, e o {@code version} de bloqueio otimista. Nada disso é
 * sigiloso; o ponto é o contrato. Enquanto os dois públicos compartilhavam o
 * record, acrescentar um campo de custo, margem ou fornecedor para a tela de
 * admin o colocava na resposta pública do catálogo no mesmo commit, sem que
 * nada no código dissesse que aquilo tinha acontecido.
 */
public record AdminProductResponseDTO(Long id, String name, String slug, String description,
		List<FabricCompositionResponseDTO> fabricCompositions, List<CareInstructionResponseDTO> careInstructions,
		BigDecimal price, BigDecimal promotionalPrice, CollectionResponseDTO collection, Category category,
		TargetAudience targetAudience, boolean active, boolean featured, List<AdminProductColorResponseDTO> colors,
		LocalDateTime deletedAt) {
}
