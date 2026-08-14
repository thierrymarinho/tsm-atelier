package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.StockChangeReason;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * As duas formas de mexer no estoque, que parecem uma só e não são.
 *
 * <p>
 * {@code delta} é um movimento: "chegaram 20". Operações concorrentes somam em
 * vez de se sobrescrever, então não precisa de versão — é a mesma lógica
 * aditiva que OrderService.restoreStock já usa ao cancelar um pedido.
 *
 * <p>
 * {@code absolute} é uma contagem: "tem 7 na prateleira". O número não é
 * derivável do estado atual, e se o sistema discordar alguém precisa saber
 * disso antes de gravar — por isso exige a {@code version} devolvida pelo GET,
 * e devolve 409 quando ela envelheceu.
 *
 * <p>
 * Não dá para simular a contagem com delta: calcular {@code contado − exibido}
 * no cliente é fazer a conta sobre uma leitura possivelmente vencida, que é o
 * lost update de novo, agora disfarçado.
 */
public record StockAdjustmentRequestDTO(Integer delta,
		@Min(value = 0, message = "Stock cannot be negative") Integer absolute, Long version,
		@NotNull(message = "Reason is required") StockChangeReason reason) {

	@AssertTrue(message = "Send exactly one of 'delta' (non-zero) or 'absolute'.")
	public boolean isSingleMeaningfulOperation() {
		if ((delta == null) == (absolute == null)) {
			return false;
		}
		return delta == null || delta != 0;
	}

	@AssertTrue(message = "'absolute' requires the 'version' returned by the API for this SKU.")
	public boolean isCountBackedByAVersion() {
		return absolute == null || version != null;
	}
}
