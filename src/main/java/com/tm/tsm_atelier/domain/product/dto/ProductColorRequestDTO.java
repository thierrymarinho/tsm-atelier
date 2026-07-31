package com.tm.tsm_atelier.domain.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductColorRequestDTO(Long id,

		@NotBlank(message = "Color name is required") @Size(max = 100, message = "Color name cannot exceed 100 characters") String colorName,

		@NotBlank(message = "Color hex code is required") @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Invalid HEX color format") String colorHex,

		@Size(max = 500, message = "Cover image URL cannot exceed 500 characters") String coverImageUrl,
		@Size(max = 500, message = "Hover image URL cannot exceed 500 characters") String hoverImageUrl,
		List<@Size(max = 500, message = "Gallery image URL cannot exceed 500 characters") String> galleryImages,

		@NotEmpty(message = "Color must have at least one SKU (size)") @Valid List<ProductSKURequestDTO> skus) {
}
