package com.tm.tsm_atelier.domain.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * O @Valid e o que faz as restricoes de CartItemRequestDTO valerem para os
 * itens da lista. Sem ele o @NotNull valida apenas a lista em si, e um item com
 * quantity nula chegava ao service e estourava no unboxing — 500 para uma
 * requisicao malformada.
 */
public record CartSyncRequestDTO(
		@NotNull(message = "Items list cannot be null") @Valid List<CartItemRequestDTO> items) {
}
