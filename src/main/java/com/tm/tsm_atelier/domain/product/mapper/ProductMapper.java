package com.tm.tsm_atelier.domain.product.mapper;

import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "slug", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "collection", ignore = true)
	@Mapping(target = "colors", ignore = true)
	Product toEntity(ProductRequestDTO request);

	ProductResponseDTO toResponse(Product entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "slug", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "collection", ignore = true)
	@Mapping(target = "colors", ignore = true)
	void updateEntityFromRequest(ProductRequestDTO request, @org.mapstruct.MappingTarget Product entity);

	default com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO toSummary(Product entity) {
		if (entity == null) {
			return null;
		}

		String coverImage = null;
		String hoverImage = null;
		java.util.List<String> colorsHex = new java.util.ArrayList<>();

		if (entity.getColors() != null && !entity.getColors().isEmpty()) {
			com.tm.tsm_atelier.domain.product.entity.ProductColor firstColor = entity.getColors().iterator().next();
			coverImage = firstColor.getCoverImageUrl();
			hoverImage = firstColor.getHoverImageUrl();

			for (com.tm.tsm_atelier.domain.product.entity.ProductColor color : entity.getColors()) {
				if (color.getColorHex() != null) {
					colorsHex.add(color.getColorHex());
				}
			}
		}

		return new com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO(entity.getId(), entity.getName(),
				entity.getSlug(), entity.getPrice(), entity.isFeatured(), coverImage, hoverImage, colorsHex);
	}
}
