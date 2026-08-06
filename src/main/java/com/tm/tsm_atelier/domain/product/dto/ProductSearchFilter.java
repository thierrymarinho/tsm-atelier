package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;

/**
 * Os filtros da busca de produtos viajam juntos, num objeto so, e nao como oito
 * parametros soltos repetidos nos dois controllers e nos dois metodos de
 * service.
 *
 * <p>
 * Isso tambem e o que mantem o cache correto. A chave de
 * {@code ProductService.searchCatalog} e derivada dos parametros do metodo, e o
 * toString() de um record inclui todos os componentes automaticamente — um
 * filtro novo entra na chave sozinho. Enquanto a chave era uma lista escrita a
 * mao, adicionar um filtro sem lembrar de adicionalo a chave fazia duas buscas
 * diferentes compartilharem a mesma entrada no Redis.
 *
 * <p>
 * Campo nulo significa "nao filtra por isso".
 */
public record ProductSearchFilter(String searchTerm, Category category, TargetAudience targetAudience,
		Long collectionId, BigDecimal minPrice, BigDecimal maxPrice, Boolean isFeatured, Boolean onSale) {
}
