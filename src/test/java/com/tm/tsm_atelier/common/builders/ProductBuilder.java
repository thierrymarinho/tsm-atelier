package com.tm.tsm_atelier.common.builders;

import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.product.entity.FabricComposition;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.entity.ProductColor;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductBuilder {

	private Long id = 1L;
	private String name = "Camiseta Básica Premium";
	private String description = "Camiseta 100% algodão egípcio";
	private List<FabricComposition> fabricCompositions = List.of(new FabricComposition("Algodão", 100));
	private List<String> careInstructions = new ArrayList<>(List.of("Lavar a mão", "Não usar alvejante"));
	private BigDecimal price = new BigDecimal("150.00");
	private BigDecimal promotionalPrice = null;
	private Collection collection = null;
	private Category category = Category.T_SHIRTS;
	private TargetAudience targetAudience = TargetAudience.MEN;
	private boolean active = true;
	private List<ProductColor> colors = new ArrayList<>();

	public static ProductBuilder aProduct() {
		return new ProductBuilder();
	}

	public ProductBuilder withId(Long id) {
		this.id = id;
		return this;
	}

	public ProductBuilder withName(String name) {
		this.name = name;
		return this;
	}

	public ProductBuilder withPromotionalPrice(BigDecimal promotionalPrice) {
		this.promotionalPrice = promotionalPrice;
		return this;
	}

	public ProductBuilder withPrice(BigDecimal price) {
		this.price = price;
		return this;
	}

	public ProductBuilder withCollection(Collection collection) {
		this.collection = collection;
		return this;
	}

	public ProductBuilder withCategory(Category category) {
		this.category = category;
		return this;
	}

	public ProductBuilder withColors(List<ProductColor> colors) {
		this.colors = colors;
		return this;
	}

	public Product build() {
		return Product.builder().id(id).name(name).description(description)
				.fabricCompositions(new ArrayList<>(fabricCompositions))
				.careInstructions(new java.util.LinkedHashSet<>(careInstructions)).price(price)
				.promotionalPrice(promotionalPrice).collection(collection).category(category)
				.targetAudience(targetAudience).active(active).colors(new java.util.LinkedHashSet<>(colors)).build();
	}
}
