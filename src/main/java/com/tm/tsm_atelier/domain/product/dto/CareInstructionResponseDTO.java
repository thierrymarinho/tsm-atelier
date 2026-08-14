package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.CareAxis;
import com.tm.tsm_atelier.domain.product.enums.CareInstruction;

/**
 * A mesma separação de FabricCompositionResponseDTO entre constante e
 * apresentação, com o eixo a mais: é o que permite à página do produto
 * apresentar a etiqueta agrupada — lavagem, secagem, passadoria — em vez de uma
 * lista solta, sem repetir o mapa de eixos no cliente.
 */
public record CareInstructionResponseDTO(CareInstruction instruction, String label, CareAxis axis) {

	public static CareInstructionResponseDTO from(CareInstruction instruction) {
		return instruction == null
				? null
				: new CareInstructionResponseDTO(instruction, instruction.getLabel(), instruction.getAxis());
	}
}
