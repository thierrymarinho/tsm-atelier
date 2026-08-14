package com.tm.tsm_atelier.common.builders;

import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.AdminProductColorResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.AdminProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.CareInstructionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.FabricCompositionResponseDTO;
import com.tm.tsm_atelier.domain.product.enums.CareInstruction;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.Material;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AdminProductResponseDTOBuilder {

	private Long id = 1L;
	private String name = "Produto Teste";
	private String slug = "produto-teste-1";
	private String description = "Descrição do produto teste";
	private List<FabricCompositionResponseDTO> fabricCompositions = List
			.of(new FabricCompositionResponseDTO(Material.COTTON, "Algodão", 100));
	private List<CareInstructionResponseDTO> careInstructions = new ArrayList<>(
			List.of(CareInstructionResponseDTO.from(CareInstruction.HAND_WASH),
					CareInstructionResponseDTO.from(CareInstruction.DO_NOT_BLEACH)));
	private BigDecimal price = new BigDecimal("199.90");
	private BigDecimal promotionalPrice = null;
	private CollectionResponseDTO collection = null;
	private Category category = Category.JEANS;
	private TargetAudience targetAudience = TargetAudience.MEN;
	private boolean active = true;
	private boolean featured = false;
	private List<AdminProductColorResponseDTO> colors = new ArrayList<>();

	public static AdminProductResponseDTOBuilder anAdminProductResponse() {
		return new AdminProductResponseDTOBuilder();
	}

	public AdminProductResponseDTOBuilder withId(Long id) {
		this.id = id;
		return this;
	}

	public AdminProductResponseDTOBuilder withName(String name) {
		this.name = name;
		return this;
	}

	public AdminProductResponseDTOBuilder withDescription(String description) {
		this.description = description;
		return this;
	}

	public AdminProductResponseDTOBuilder withFabricCompositions(
			List<FabricCompositionResponseDTO> fabricCompositions) {
		this.fabricCompositions = fabricCompositions;
		return this;
	}

	public AdminProductResponseDTOBuilder withCareInstructions(List<CareInstructionResponseDTO> careInstructions) {
		this.careInstructions = careInstructions;
		return this;
	}

	public AdminProductResponseDTOBuilder withPromotionalPrice(BigDecimal promotionalPrice) {
		this.promotionalPrice = promotionalPrice;
		return this;
	}

	public AdminProductResponseDTOBuilder withPrice(BigDecimal price) {
		this.price = price;
		return this;
	}

	public AdminProductResponseDTOBuilder withCollection(CollectionResponseDTO collection) {
		this.collection = collection;
		return this;
	}

	public AdminProductResponseDTOBuilder withCategory(Category category) {
		this.category = category;
		return this;
	}

	public AdminProductResponseDTOBuilder withTargetAudience(TargetAudience targetAudience) {
		this.targetAudience = targetAudience;
		return this;
	}

	public AdminProductResponseDTOBuilder withActive(boolean active) {
		this.active = active;
		return this;
	}

	public AdminProductResponseDTOBuilder withColors(List<AdminProductColorResponseDTO> colors) {
		this.colors = colors;
		return this;
	}

	public AdminProductResponseDTO build() {
		return new AdminProductResponseDTO(id, name, slug, description, fabricCompositions, careInstructions, price,
				promotionalPrice, collection, category, targetAudience, active, featured, colors, null);
	}
}
