package com.tm.tsm_atelier.domain.product.dto;

import java.util.List;

public record ProductColorResponseDTO(Long id, String colorName, String colorHex, String coverImageUrl,
		String hoverImageUrl, List<String> galleryImages, List<ProductSKUResponseDTO> skus) {
}
