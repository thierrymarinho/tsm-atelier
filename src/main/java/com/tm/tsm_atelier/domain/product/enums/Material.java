package com.tm.tsm_atelier.domain.product.enums;

import lombok.Getter;

@Getter
public enum Material {

	// Naturais de origem vegetal.
	COTTON("Algodão"), LINEN("Linho"),

	// Naturais de origem animal.
	WOOL("Lã"), SILK("Seda"), CASHMERE("Caxemira"), LEATHER("Couro"),

	// Artificiais: celulose regenerada.
	VISCOSE("Viscose"), MODAL("Modal"), LYOCELL("Lyocell"),

	// Sintéticas. O acrílico faz o papel da lã, e o poliuretano é o que a
	// etiqueta de um "couro sintético" declara.
	POLYESTER("Poliéster"), POLYAMIDE("Poliamida"), ELASTANE("Elastano"), ACRYLIC("Acrílico"), POLYURETHANE(
			"Poliuretano");
	// @formatter:on

	private final String label;

	Material(String label) {
		this.label = label;
	}

}
