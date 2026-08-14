package com.tm.tsm_atelier.domain.product.enums;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;

@Getter
public enum CareInstruction {

	// O bloco é uma tabela: eixo, constante, rótulo. Sem os marcadores abaixo o
	// formatador empacota as constantes lado a lado até estourar a linha, e a
	// coluna do eixo — que é o que dá sentido ao agrupamento — desaparece.
	// @formatter:off
	MACHINE_WASH_COLD(CareAxis.WASH, "Lavar à máquina em água fria (até 30°C)"),
	MACHINE_WASH_WARM(CareAxis.WASH, "Lavar à máquina em água morna (até 40°C)"),
	HAND_WASH(CareAxis.WASH, "Lavar à mão"),
	DO_NOT_WASH(CareAxis.WASH, "Não lavar"),

	NON_CHLORINE_BLEACH(CareAxis.BLEACH, "Alvejar somente sem cloro"),
	DO_NOT_BLEACH(CareAxis.BLEACH, "Não usar alvejante"),

	TUMBLE_DRY_LOW(CareAxis.TUMBLE_DRY, "Secadora em temperatura baixa"),
	DO_NOT_TUMBLE_DRY(CareAxis.TUMBLE_DRY, "Não usar secadora"),

	// Secar na horizontal não é enfeite: tricô pendurado no varal deforma.
	LINE_DRY(CareAxis.NATURAL_DRY, "Secar no varal"),
	DRY_FLAT(CareAxis.NATURAL_DRY, "Secar na horizontal"),
	DRY_IN_SHADE(CareAxis.NATURAL_DRY, "Secar à sombra"),

	IRON_LOW(CareAxis.IRON, "Passar em temperatura baixa"),
	IRON_MEDIUM(CareAxis.IRON, "Passar em temperatura média"),
	DO_NOT_IRON(CareAxis.IRON, "Não passar"),

	DRY_CLEAN(CareAxis.PROFESSIONAL, "Lavagem a seco"),
	DO_NOT_DRY_CLEAN(CareAxis.PROFESSIONAL, "Não lavar a seco");

	private final CareAxis axis;

	private final String label;

	CareInstruction(CareAxis axis, String label) {
		this.axis = axis;
		this.label = label;
	}

	public static List<CareInstruction> byAxis(CareAxis axis) {
		return Arrays.stream(values()).filter(instruction -> instruction.axis == axis).toList();
	}

}
