package com.tm.tsm_atelier.domain.product.enums;

import lombok.Getter;

/**
 * Os grupos de uma etiqueta de conservação. Cada eixo aceita uma instrução: é o
 * que impede a peça de sair com "Não lavar" e "Lavar à mão" ao mesmo tempo.
 */
@Getter
public enum CareAxis {

	WASH("Lavagem"), BLEACH("Alvejamento"),

	TUMBLE_DRY("Secadora"), NATURAL_DRY("Secagem natural"),

	IRON("Passadoria"), PROFESSIONAL("Cuidado profissional");

	private final String label;

	CareAxis(String label) {
		this.label = label;
	}

}
