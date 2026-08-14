package com.tm.tsm_atelier.domain.admin.entity;

/**
 * O verbo do registro de auditoria. O substantivo fica em
 * {@link AuditedEntity}, e a separação é proposital: {@code PRODUCT_DELETED}
 * como valor único obrigaria a listar "tudo o que foi excluído" enumerando um
 * valor por tipo de entidade, e a consulta "tudo o que aconteceu com o produto
 * 42" precisa de tipo e id em colunas próprias de qualquer forma.
 */
public enum AuditAction {

	CREATED,

	UPDATED,

	DELETED,

	RESTORED,

	/** Exclusivo de pedido: o par antes/depois são dois {@code OrderStatus}. */
	STATUS_CHANGED,

	/** Exclusivo de SKU: o único que preenche {@code reason}. */
	STOCK_ADJUSTED,

	/**
	 * Separado de {@code UPDATED} porque mexe em dinheiro e porque acontece dentro
	 * do mesmo PUT que altera qualquer outro campo — sem ação própria, colocar e
	 * retirar uma promoção ficaria indistinguível de corrigir uma descrição.
	 */
	PROMOTIONAL_PRICE_CHANGED
}
