package com.tm.tsm_atelier.domain.product.enums;

/**
 * Por que o estoque mudou. O banco guarda apenas o valor final, então sem o
 * motivo viajando junto o log de ajuste registraria "de 10 para 7" sem dizer se
 * aquilo foi contagem, perda ou correção de cadastro — e é exatamente essa a
 * pergunta que se faz quando o número não bate.
 *
 * <p>
 * É também o campo que uma tabela de auditoria (MEL-05) herdaria sem reescrita.
 */
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
