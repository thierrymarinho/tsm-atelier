package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Não há skuCode aqui: o código é interno e o backend o gera.
 * <p>
 * O stockQuantity vale apenas para SKU novo, como estoque inicial. Enviá-lo com
 * id preenchido é recusadoo
 */
public record ProductSKURequestDTO(Long id,

		@NotNull(message = "Size is required") ProductSize size,

		@Min(value = 0, message = "Stock cannot be negative") Integer stockQuantity) {
}
