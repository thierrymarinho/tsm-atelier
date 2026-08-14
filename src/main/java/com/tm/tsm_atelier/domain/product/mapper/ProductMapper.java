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

/**
 * A estrategia de nulo e declarada de proposito, e nao herdada do default. Com
 * IGNORE, um promotionalPrice nulo vindo do request seria descartado e a
 * promocao ficaria impossivel de remover — o admin salvaria, receberia 200 e o
 * preco promocional continuaria la, sem erro nenhum.
 */
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
	void updateEntityFromRequest(ProductRequestDTO request, @MappingTarget Product entity);

	AdminProductResponseDTO toAdminResponse(Product entity);

	/**
	 * Explícito porque {@code label} não existe na entidade — ele vem do enum. Sem
	 * este método o MapStruct falharia a compilação com "Unmapped target property",
	 * que é o comportamento certo: o rótulo não é dado, é derivado.
	 */
	@Mapping(target = "label", source = "material.label")
	FabricCompositionResponseDTO toFabricCompositionResponse(FabricComposition composition);

	/** Pelo mesmo motivo do anterior: rótulo e eixo são derivados da constante. */
	default CareInstructionResponseDTO toCareInstructionResponse(CareInstruction instruction) {
		return CareInstructionResponseDTO.from(instruction);
	}

	/**
	 * A resposta do catalogo e derivada da do admin, e a derivacao <em>e</em> o
	 * filtro: cor e SKU removidos ficam para tras porque nao ha para onde
	 * copia-los.
	 *
	 * <p>
	 * O filtro em memoria continua necessario mesmo com o {@code @SQLRestriction}
	 * das entidades: ele vale para o carregamento do banco, e nao para uma colecao
	 * que ja esta no contexto de persistencia com o item marcado como removido
	 * nesta mesma transacao.
	 */
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

	/** O que os dois cards extraem das cores, para nao duplicar a varredura. */
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
