package com.tm.tsm_atelier.common.builders;

import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.FabricCompositionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductColorResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductResponseDTOBuilder {

	private Long id = 1L;
	private String name = "Produto Teste";
	private String slug = "produto-teste-1";
	private String description = "Descrição do produto teste";
	private List<FabricCompositionResponseDTO> fabricCompositions = List
			.of(new FabricCompositionResponseDTO("Algodão", 100));
	private List<String> careInstructions = new ArrayList<>(List.of("Lavar à mão", "Não usar alvejante"));
	private BigDecimal price = new BigDecimal("199.90");
	private CollectionResponseDTO collection = null; // null por padrão para testes mais simples
	private Category category = Category.JEANS;
	private TargetAudience targetAudience = TargetAudience.MEN;
	private boolean active = true;
	private boolean featured = false;
	private List<ProductColorResponseDTO> colors = new ArrayList<>();

	public static ProductResponseDTOBuilder aProductResponse() {
		return new ProductResponseDTOBuilder();
	}

	public ProductResponseDTOBuilder withId(Long id) {
		this.id = id;
		return this;
	}

	public ProductResponseDTOBuilder withName(String name) {
		this.name = name;
		return this;
	}

	public ProductResponseDTOBuilder withDescription(String description) {
		this.description = description;
		return this;
	}

	public ProductResponseDTOBuilder withFabricCompositions(List<FabricCompositionResponseDTO> fabricCompositions) {
		this.fabricCompositions = fabricCompositions;
		return this;
	}

	public ProductResponseDTOBuilder withCareInstructions(List<String> careInstructions) {
		this.careInstructions = careInstructions;
		return this;
	}

	public ProductResponseDTOBuilder withPrice(BigDecimal price) {
		this.price = price;
		return this;
	}

	public ProductResponseDTOBuilder withCollection(CollectionResponseDTO collection) {
		this.collection = collection;
		return this;
	}

	public ProductResponseDTOBuilder withCategory(Category category) {
		this.category = category;
		return this;
	}

	public ProductResponseDTOBuilder withTargetAudience(TargetAudience targetAudience) {
		this.targetAudience = targetAudience;
		return this;
	}

	public ProductResponseDTOBuilder withActive(boolean active) {
		this.active = active;
		return this;
	}

	public ProductResponseDTOBuilder withColors(List<ProductColorResponseDTO> colors) {
		this.colors = colors;
		return this;
	}

	public ProductResponseDTO build() {
		return new ProductResponseDTO(id, name, slug, description, fabricCompositions, careInstructions, price,
				collection, category, targetAudience, active, featured, colors, null);
	}
}
