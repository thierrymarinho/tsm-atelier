package com.tm.tsm_atelier.common.builders;

import com.tm.tsm_atelier.domain.product.dto.FabricCompositionRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductColorRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSKURequestDTO;
import com.tm.tsm_atelier.domain.product.enums.CareInstruction;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.Material;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductRequestDTOBuilder {

	private String name = "Calça Jeans Skinny";
	private String description = "Calça jeans premium";
	private List<FabricCompositionRequestDTO> fabricCompositions = List.of(
			new FabricCompositionRequestDTO(Material.COTTON, 98),
			new FabricCompositionRequestDTO(Material.ELASTANE, 2));
	private List<CareInstruction> careInstructions = new ArrayList<>(List.of(CareInstruction.MACHINE_WASH_COLD));
	private BigDecimal price = new BigDecimal("200.00");
	private BigDecimal promotionalPrice = null;
	private Long collectionId = 1L;
	private Category category = Category.DRESSES;
	private TargetAudience targetAudience = TargetAudience.WOMEN;
	private boolean active = true;
	private boolean featured = false;
	private List<ProductColorRequestDTO> colors = new ArrayList<>(List.of(new ProductColorRequestDTO(null, "Azul",
			"#0000FF", "http://cover.jpg", "http://hover.jpg", new ArrayList<>(),
			List.of(new ProductSKURequestDTO(null, com.tm.tsm_atelier.domain.product.enums.ProductSize.M, 10)))));

	public static ProductRequestDTOBuilder aProductRequest() {
		return new ProductRequestDTOBuilder();
	}

	public ProductRequestDTOBuilder withName(String name) {
		this.name = name;
		return this;
	}

	public ProductRequestDTOBuilder withPromotionalPrice(BigDecimal promotionalPrice) {
		this.promotionalPrice = promotionalPrice;
		return this;
	}

	public ProductRequestDTOBuilder withFabricCompositions(List<FabricCompositionRequestDTO> fabricCompositions) {
		this.fabricCompositions = fabricCompositions;
		return this;
	}

	public ProductRequestDTOBuilder withCareInstructions(List<CareInstruction> careInstructions) {
		this.careInstructions = careInstructions;
		return this;
	}

	public ProductRequestDTOBuilder withPrice(BigDecimal price) {
		this.price = price;
		return this;
	}

	public ProductRequestDTOBuilder withCollectionId(Long collectionId) {
		this.collectionId = collectionId;
		return this;
	}

	public ProductRequestDTOBuilder withColors(List<ProductColorRequestDTO> colors) {
		this.colors = colors;
		return this;
	}

	public ProductRequestDTO build() {
		return new ProductRequestDTO(name, description, fabricCompositions, careInstructions, price, promotionalPrice,
				collectionId, category, targetAudience, active, featured, colors);
	}
}
