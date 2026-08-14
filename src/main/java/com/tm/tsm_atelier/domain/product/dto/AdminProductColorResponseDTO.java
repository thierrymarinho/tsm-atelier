package com.tm.tsm_atelier.domain.product.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminProductColorResponseDTO(Long id, String colorName, String colorHex, String coverImageUrl,
		String hoverImageUrl, List<String> galleryImages, List<AdminProductSKUResponseDTO> skus,
		LocalDateTime deletedAt) {
}
