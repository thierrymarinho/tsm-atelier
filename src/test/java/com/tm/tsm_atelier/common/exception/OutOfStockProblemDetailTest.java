package com.tm.tsm_atelier.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.common.exception.custom.OutOfStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

/**
 * O que o cliente do front lê do corpo do 409. Os campos novos são aditivos:
 * status, título e {@code availableQuantity} continuam onde estavam, porque há
 * front em produção lendo os três.
 */
@DisplayName("ProblemDetail do 409 de estoque")
class OutOfStockProblemDetailTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	@DisplayName("Traz skuId e reason junto do availableQuantity que já existia")
	void shouldCarryTheItemIdentityAndTheReason() {
		ProblemDetail problem = handler
				.handleOutOfStock(new OutOfStockException("Out of stock for SKU: TSM-000014. Available: 0", 0, 14L,
						OutOfStockException.Reason.INSUFFICIENT_STOCK));

		assertThat(problem.getStatus()).isEqualTo(409);
		assertThat(problem.getTitle()).isEqualTo("Out of stock");
		assertThat(problem.getDetail()).isEqualTo("Out of stock for SKU: TSM-000014. Available: 0");

		assertThat(problem.getProperties()).containsEntry("availableQuantity", 0).containsEntry("skuId", 14L)
				.containsEntry("reason", "INSUFFICIENT_STOCK");
	}

	@Test
	@DisplayName("Só o teto por pedido manda maxUnitsPerItem")
	void shouldSendTheCapOnlyWhereItApplies() {
		ProblemDetail capped = handler.handleOutOfStock(new OutOfStockException("Maximum 10 units per item", 10, 14L,
				OutOfStockException.Reason.MAX_UNITS_PER_ITEM, 10));

		assertThat(capped.getProperties()).containsEntry("reason", "MAX_UNITS_PER_ITEM")
				.containsEntry("maxUnitsPerItem", 10);

		ProblemDetail unavailable = handler.handleOutOfStock(new OutOfStockException(
				"This product is no longer available.", 0, 14L, OutOfStockException.Reason.PRODUCT_UNAVAILABLE));

		// Mandar a chave com null convidaria o front a mostrar "maximo de null".
		assertThat(unavailable.getProperties()).doesNotContainKey("maxUnitsPerItem");
	}
}
