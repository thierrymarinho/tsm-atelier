package com.tm.tsm_atelier.domain.product.mapper;

import static com.tm.tsm_atelier.common.builders.ProductBuilder.aProduct;
import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.domain.product.dto.AdminProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductColorResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSKUResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.entity.ProductColor;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Um único record servindo loja e painel publica na vitrine tudo o que o admin
 * vier a precisar. Estes testes são a parte que sobrevive à próxima edição: a
 * ausência dos campos é verificada na forma do tipo, e não numa checagem que
 * alguém pode esquecer de repetir.
 */
@DisplayName("Catalog response shape")
class CatalogResponseShapeTest {

	private final ProductMapper mapper = new ProductMapperImpl();

	@Test
	@DisplayName("Should not publish internal bookkeeping in the storefront contract")
	void catalogCarriesNoAdminOnlyFields() {
		assertThat(componentsOf(ProductResponseDTO.class)).doesNotContain("deletedAt");
		assertThat(componentsOf(ProductColorResponseDTO.class)).doesNotContain("deletedAt");
		assertThat(componentsOf(ProductSummaryDTO.class)).doesNotContain("deletedAt");

		// version é o token de bloqueio otimista, usado só pela contagem de
		// inventário do PATCH de estoque.
		assertThat(componentsOf(ProductSKUResponseDTO.class)).doesNotContain("deletedAt", "version");
	}

	@Test
	@DisplayName("Should keep the admin view complete")
	void adminKeepsEverything() {
		assertThat(componentsOf(AdminProductResponseDTO.class)).contains("deletedAt");
	}

	/**
	 * O outro lado da separação: os campos não sumiram só do contrato, o conteúdo
	 * removido também não chega ao catálogo. O @SQLRestriction das entidades não
	 * cobre este caso — ele vale para o carregamento do banco, e aqui a cor já está
	 * na coleção em memória.
	 */
	@Test
	@DisplayName("Should leave removed colours and skus out of the catalog response")
	void catalogDropsRemovedBranches() {
		// Ids distintos não são detalhe: ProductColor.equals é por id, e cores com o
		// mesmo id colapsam no Set antes de o mapper ver qualquer coisa.
		Product product = aProduct().withColors(List.of(colour(1L, "Azul", null, null),
				colour(2L, "Verde", LocalDateTime.now(), null), colour(3L, "Preto", null, LocalDateTime.now())))
				.build();

		AdminProductResponseDTO seenByAdmin = mapper.toAdminResponse(product);
		ProductResponseDTO seenByShop = mapper.toCatalogResponse(product);

		assertThat(seenByAdmin.colors()).hasSize(3);
		assertThat(seenByShop.colors()).extracting(ProductColorResponseDTO::colorName).containsExactly("Azul", "Preto");

		// "Preto" sobrevive porque a cor está viva; o SKU dela é que foi removido.
		assertThat(seenByShop.colors().getLast().skus()).isEmpty();
	}

	private ProductColor colour(Long id, String name, LocalDateTime colourRemovedAt, LocalDateTime skuRemovedAt) {
		ProductColor color = ProductColor.builder().id(id).colorName(name).colorHex("#000000")
				.galleryImages(new LinkedHashSet<>()).deletedAt(colourRemovedAt).skus(new LinkedHashSet<>()).build();

		color.getSkus().add(ProductSKU.builder().id(id).size(ProductSize.M).skuCode(name + "-M").stockQuantity(5)
				.version(0L).deletedAt(skuRemovedAt).build());

		return color;
	}

	private List<String> componentsOf(Class<?> record) {
		return java.util.Arrays.stream(record.getRecordComponents()).map(RecordComponent::getName).toList();
	}
}
