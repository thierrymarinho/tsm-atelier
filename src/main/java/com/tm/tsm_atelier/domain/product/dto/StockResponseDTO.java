package com.tm.tsm_atelier.domain.product.dto;

/**
 * O estado do SKU depois do ajuste, para o painel atualizar a linha sem refazer
 * o GET do produto inteiro.
 *
 * <p>
 * A {@code version} vem daqui já incrementada de propósito: devolver a anterior
 * faria a próxima contagem enviada pela mesma tela bater de frente com um 409
 * causado pela própria resposta anterior.
 */
public record StockResponseDTO(Long skuId, String skuCode, Integer stockQuantity, Long version) {
}
