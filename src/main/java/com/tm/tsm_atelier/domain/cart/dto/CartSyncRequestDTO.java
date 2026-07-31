package com.tm.tsm_atelier.domain.cart.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CartSyncRequestDTO(@NotNull(message = "Items list cannot be null") List<CartItemRequestDTO> items) {
}
