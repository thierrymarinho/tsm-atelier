package com.tm.tsm_atelier.domain.product.entity;

import com.tm.tsm_atelier.domain.product.enums.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FabricComposition {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Material material;

	@Column(nullable = false)
	private Integer percentage;
}
