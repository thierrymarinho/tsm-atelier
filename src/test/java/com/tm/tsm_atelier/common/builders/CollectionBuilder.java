package com.tm.tsm_atelier.common.builders;

import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;

public class CollectionBuilder {

	private Long id = 1L;
	private String name = "Coleção Verão 2026";
	private boolean active = true;
	private String heroImageUrl = "http://res.cloudinary.com/demo/image/upload/sample.jpg";
	private String portraitImageUrl = "http://res.cloudinary.com/demo/image/upload/sample.jpg";
	private String squareImageUrl = "http://res.cloudinary.com/demo/image/upload/sample.jpg";
	private DisplayPosition displayPosition = DisplayPosition.NONE;
	private Integer displayOrder = 0;
	private TargetAudience targetAudience = TargetAudience.WOMEN;

	public static CollectionBuilder aCollection() {
		return new CollectionBuilder();
	}

	public CollectionBuilder withId(Long id) {
		this.id = id;
		return this;
	}

	public CollectionBuilder withName(String name) {
		this.name = name;
		return this;
	}

	public CollectionBuilder withActive(boolean active) {
		this.active = active;
		return this;
	}

	public CollectionBuilder withHeroImageUrl(String heroImageUrl) {
		this.heroImageUrl = heroImageUrl;
		return this;
	}

	public CollectionBuilder withPortraitImageUrl(String portraitImageUrl) {
		this.portraitImageUrl = portraitImageUrl;
		return this;
	}

	public CollectionBuilder withSquareImageUrl(String squareImageUrl) {
		this.squareImageUrl = squareImageUrl;
		return this;
	}

	public CollectionBuilder withDisplayPosition(DisplayPosition displayPosition) {
		this.displayPosition = displayPosition;
		return this;
	}

	public CollectionBuilder withDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
		return this;
	}

	public CollectionBuilder withTargetAudience(TargetAudience targetAudience) {
		this.targetAudience = targetAudience;
		return this;
	}

	public Collection build() {
		return Collection.builder().id(id).name(name).active(active).heroImageUrl(heroImageUrl)
				.portraitImageUrl(portraitImageUrl).squareImageUrl(squareImageUrl).displayPosition(displayPosition)
				.displayOrder(displayOrder).targetAudience(targetAudience).build();
	}
}
