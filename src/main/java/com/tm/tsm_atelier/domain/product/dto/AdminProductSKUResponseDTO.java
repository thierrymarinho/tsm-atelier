package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import java.time.LocalDateTime;

/**
 * O SKU como o painel precisa vê-lo. Os dois campos que o separam da versão de
 * catálogo são escrituração interna:
 *
 * <ul>
 * <li>{@code version} — o token de bloqueio otimista, usado só pela contagem de
 * inventário do {@code PATCH /admin/skus/{id}/stock}. Não tem significado
 * nenhum para quem navega na loja.
 * <li>{@code deletedAt} — o catálogo nunca devolve SKU removido, então lá o
 * campo é constantemente nulo.
 * </ul>
 */
public record AdminProductSKUResponseDTO(Long id, Long version, ProductSize size, String skuCode, Integer stockQuantity,
		LocalDateTime deletedAt) {
}
