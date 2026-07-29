package com.tm.tsm_atelier.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CheckoutRequestDTO(@NotNull(message = "Address ID is required") Long addressId,

		@NotEmpty(message = "Items cannot be empty") @Valid List<CheckoutItemDTO> items) {
}
