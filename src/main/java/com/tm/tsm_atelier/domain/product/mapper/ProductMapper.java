package com.tm.tsm_atelier.domain.product.mapper;

import com.tm.tsm_atelier.domain.product.dto.AdminProductColorResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.AdminProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.AdminProductSummaryDTO;
import com.tm.tsm_atelier.domain.product.dto.CareInstructionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.FabricCompositionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductColorResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSKUResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO;
import com.tm.tsm_atelier.domain.product.entity.FabricComposition;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.entity.ProductColor;
import com.tm.tsm_atelier.domain.product.enums.CareInstruction;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
public interface ProductMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "slug", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "collection", ignore = true)
	@Mapping(target = "colors", ignore = true)
	Product toEntity(ProductRequestDTO request);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "slug", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "collection", ignore = true)
	@Mapping(target = "colors", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void updateEntityFromRequest(ProductRequestDTO request, @MappingTarget Product entity);

	AdminProductResponseDTO toAdminResponse(Product entity);

	@Mapping(target = "label", source = "material.label")
	FabricCompositionResponseDTO toFabricCompositionResponse(FabricComposition composition);

	default CareInstructionResponseDTO toCareInstructionResponse(CareInstruction instruction) {
		return CareInstructionResponseDTO.from(instruction);
	}

	default ProductResponseDTO toCatalogResponse(Product entity) {
		AdminProductResponseDTO full = toAdminResponse(entity);
		if (full == null) {
			return null;
		}

		List<ProductColorResponseDTO> activeColors = new ArrayList<>();
		if (full.colors() != null) {
			for (AdminProductColorResponseDTO color : full.colors()) {
				if (color.deletedAt() == null) {
					activeColors.add(toCatalogColor(color));
				}
			}
		}

		return new ProductResponseDTO(full.id(), full.name(), full.slug(), full.description(),
				full.fabricCompositions(), full.careInstructions(), full.price(), full.promotionalPrice(),
				full.collection(), full.category(), full.targetAudience(), full.active(), full.featured(),
				activeColors);
	}

	private ProductColorResponseDTO toCatalogColor(AdminProductColorResponseDTO color) {
		List<ProductSKUResponseDTO> activeSkus = color.skus() == null
				? null
				: color.skus().stream().filter(sku -> sku.deletedAt() == null)
						.map(sku -> new ProductSKUResponseDTO(sku.id(), sku.size(), sku.skuCode(), sku.stockQuantity()))
						.toList();

		return new ProductColorResponseDTO(color.id(), color.colorName(), color.colorHex(), color.coverImageUrl(),
				color.hoverImageUrl(), color.galleryImages(), activeSkus);
	}

	default AdminProductSummaryDTO toAdminSummary(Product entity) {
		if (entity == null) {
			return null;
		}

		Cover cover = coverOf(entity);
		return new AdminProductSummaryDTO(entity.getId(), entity.getName(), entity.getSlug(), entity.getPrice(),
				entity.getPromotionalPrice(), entity.isFeatured(), cover.coverImageUrl(), cover.hoverImageUrl(),
				cover.colorsHex(), entity.getDeletedAt(), entity.isActive());
	}

	default ProductSummaryDTO toSummary(Product entity) {
		if (entity == null) {
			return null;
		}

		Cover cover = coverOf(entity);
		return new ProductSummaryDTO(entity.getId(), entity.getName(), entity.getSlug(), entity.getPrice(),
				entity.getPromotionalPrice(), entity.isFeatured(), cover.coverImageUrl(), cover.hoverImageUrl(),
				cover.colorsHex(), entity.isActive());
	}

	record Cover(String coverImageUrl, String hoverImageUrl, List<String> colorsHex) {
	}

	private Cover coverOf(Product entity) {
		if (entity.getColors() == null || entity.getColors().isEmpty()) {
			return new Cover(null, null, List.of());
		}

		ProductColor first = entity.getColors().iterator().next();
		List<String> colorsHex = entity.getColors().stream().map(ProductColor::getColorHex).filter(hex -> hex != null)
				.toList();

		return new Cover(first.getCoverImageUrl(), first.getHoverImageUrl(), colorsHex);
	}
}
