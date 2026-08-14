package com.tm.tsm_atelier.domain.admin.entity;

/**
 * O que foi alterado. Enum em vez de texto livre porque este é um dos filtros
 * da tela de histórico: {@code "Product"} e {@code "PRODUCT"} gravados em
 * momentos diferentes produziriam uma busca que devolve metade do que existe,
 * sem erro nenhum.
 */
public enum AuditedEntity {

	PRODUCT,

	PRODUCT_SKU,

	COLLECTION,

	ORDER
}
