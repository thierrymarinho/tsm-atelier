package com.tm.tsm_atelier.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutItemDTO(@NotNull(message = "SKU ID is required") Long skuId,

		@NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
}
