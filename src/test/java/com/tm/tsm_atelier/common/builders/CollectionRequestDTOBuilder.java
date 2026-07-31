package com.tm.tsm_atelier.common.builders;

import com.tm.tsm_atelier.domain.collection.dto.CollectionRequestDTO;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;

public class CollectionRequestDTOBuilder {

	private String name = "Coleção Inverno 2026";
	private boolean active = true;
	private String description = "Descrição da coleção";
	private String heroImageUrl = "http://res.cloudinary.com/demo/image/upload/sample.jpg";
	private String portraitImageUrl = "http://res.cloudinary.com/demo/image/upload/sample.jpg";
	private String squareImageUrl = "http://res.cloudinary.com/demo/image/upload/sample.jpg";
	private DisplayPosition displayPosition = DisplayPosition.NONE;
	private Integer displayOrder = 0;
	private TargetAudience targetAudience = TargetAudience.WOMEN;

	public static CollectionRequestDTOBuilder aCollectionRequest() {
		return new CollectionRequestDTOBuilder();
	}

	public CollectionRequestDTOBuilder withName(String name) {
		this.name = name;
		return this;
	}

	public CollectionRequestDTOBuilder withActive(boolean active) {
		this.active = active;
		return this;
	}

	public CollectionRequestDTOBuilder withDescription(String description) {
		this.description = description;
		return this;
	}

	public CollectionRequestDTOBuilder withHeroImageUrl(String heroImageUrl) {
		this.heroImageUrl = heroImageUrl;
		return this;
	}

	public CollectionRequestDTOBuilder withPortraitImageUrl(String portraitImageUrl) {
		this.portraitImageUrl = portraitImageUrl;
		return this;
	}

	public CollectionRequestDTOBuilder withSquareImageUrl(String squareImageUrl) {
		this.squareImageUrl = squareImageUrl;
		return this;
	}

	public CollectionRequestDTOBuilder withDisplayPosition(DisplayPosition displayPosition) {
		this.displayPosition = displayPosition;
		return this;
	}

	public CollectionRequestDTOBuilder withDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
		return this;
	}

	public CollectionRequestDTOBuilder withTargetAudience(TargetAudience targetAudience) {
		this.targetAudience = targetAudience;
		return this;
	}

	public CollectionRequestDTO build() {
		return new CollectionRequestDTO(name, active, description, heroImageUrl, portraitImageUrl, squareImageUrl,
				displayPosition, displayOrder, targetAudience);
	}
}
