package com.tm.tsm_atelier.domain.product.dto;

import com.tm.tsm_atelier.domain.product.enums.CareAxis;
import com.tm.tsm_atelier.domain.product.enums.CareInstruction;
import java.util.List;

/**
 * O vocabulário de cuidados servido já agrupado, e não como lista plana. O
 * agrupamento é o próprio contrato: cada eixo aceita uma instrução, então o
 * formulário do admin vira um campo por eixo e a combinação contraditória —
 * "Não lavar" com "Lavar à mão" — deixa de ser possível de escolher, em vez de
 * ser recusada depois de enviada.
 */
public record CareAxisOptionsDTO(String axis, String label, List<Option> options) {

	public record Option(String name, String label) {
	}

	public static CareAxisOptionsDTO from(CareAxis axis) {
		List<Option> options = CareInstruction.byAxis(axis).stream()
				.map(instruction -> new Option(instruction.name(), instruction.getLabel())).toList();

		return new CareAxisOptionsDTO(axis.name(), axis.getLabel(), options);
	}
}
