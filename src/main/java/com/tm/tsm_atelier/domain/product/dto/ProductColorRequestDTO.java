package com.tm.tsm_atelier.domain.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ProductColorRequestDTO(Long id,

		@NotBlank(message = "Color name is required") String colorName,

		@NotBlank(message = "Color hex code is required") String colorHex,

		String coverImageUrl, String hoverImageUrl, List<String> galleryImages,

		@NotEmpty(message = "Color must have at least one SKU (size)") @Valid List<ProductSKURequestDTO> skus) {
}
