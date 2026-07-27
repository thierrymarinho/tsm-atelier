package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductSKURequestDTO(Long id,

		@NotNull(message = "Size is required") ProductSize size,

		@NotBlank(message = "SKU code is required") String skuCode,

		@NotNull(message = "Stock quantity is required") @Min(value = 0, message = "Stock cannot be negative") Integer stockQuantity) {
}
