package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductSKURequestDTO(Long id,

		@NotNull(message = "Size is required") ProductSize size,

		@NotBlank(message = "SKU code is required") @Size(max = 100, message = "SKU code cannot exceed 100 characters") String skuCode,

		@NotNull(message = "Stock quantity is required") @Min(value = 0, message = "Stock cannot be negative") Integer stockQuantity) {
}
