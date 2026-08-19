package com.tm.tsm_atelier.common.exception.custom;

import lombok.Getter;

/**
 * O {@code message} daqui é texto de log: sai em inglês e nomeia o item pelo
 * código interno do SKU. Quem escreve a frase para o cliente é o front, que tem
 * nome, cor e tamanho em mãos e sabe em qual tela o cliente está — por isso o
 * que viaja junto é o dado bruto: qual item falhou ({@code skuId}) e por quê
 * ({@code reason}).
 *
 * As três causas produzem o mesmo 409. Sem o {@code reason} elas só se
 * distinguiam por aritmética sobre o {@code availableQuantity}, inferência que
 * dependia do teto valer exatamente 10 e da ordem das checagens em
 * {@code CartService.addItem} — e que quebraria em silêncio ao mudar qualquer
 * um dos dois. Produto desativado, em particular, era indistinguível de estoque
 * zerado, e as duas coisas não são a mesma: uma volta amanhã, a outra não
 * volta.
 */
@Getter
public class OutOfStockException extends RuntimeException {

	private final Integer availableQuantity;
	private final Long skuId;
	private final Reason reason;
	private final Integer maxUnitsPerItem;

	public OutOfStockException(String message, Integer availableQuantity, Long skuId, Reason reason) {
		this(message, availableQuantity, skuId, reason, null);
	}

	public OutOfStockException(String message, Integer availableQuantity, Long skuId, Reason reason,
			Integer maxUnitsPerItem) {
		super(message);
		this.availableQuantity = availableQuantity;
		this.skuId = skuId;
		this.reason = reason;
		this.maxUnitsPerItem = maxUnitsPerItem;
	}

	public enum Reason {
		MAX_UNITS_PER_ITEM, INSUFFICIENT_STOCK, PRODUCT_UNAVAILABLE
	}

}
