package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.Material;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FabricCompositionRequestDTO(@NotNull(message = "Material is required") Material material,
		@NotNull(message = "Percentage is required") @Min(value = 1, message = "Percentage must be at least 1") @Max(value = 100, message = "Percentage cannot exceed 100") Integer percentage) {
}
