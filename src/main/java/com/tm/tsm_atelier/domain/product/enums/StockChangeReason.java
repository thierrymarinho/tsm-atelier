package com.tm.tsm_atelier.domain.product.enums;

public enum StockChangeReason {

	/** Chegou mercadoria. */
	RESTOCK,

	/** Contagem física: o número contado vira o valor absoluto. */
	INVENTORY_COUNT,

	/** Devolução de cliente que volta ao estoque vendável. */
	RETURN,

	/** Peça danificada, retirada de venda. */
	DAMAGE,

	/** Extravio. */
	LOSS,

	/** Correção de erro de cadastro, sem movimento físico. */
	CORRECTION
}
