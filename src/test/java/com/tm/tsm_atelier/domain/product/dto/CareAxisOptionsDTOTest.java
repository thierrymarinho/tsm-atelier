package com.tm.tsm_atelier.domain.product.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.domain.product.enums.CareAxis;
import com.tm.tsm_atelier.domain.product.enums.CareInstruction;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Care instruction options")
class CareAxisOptionsDTOTest {

	/**
	 * O agrupamento é o que o formulário do admin consome: uma instrução fora de
	 * qualquer eixo simplesmente não apareceria na tela, e o admin não teria como
	 * saber que ela existe. Uma constante nova sem eixo declarado quebra aqui, e
	 * não em produção.
	 */
	@Test
	@DisplayName("Every instruction is served under exactly one axis")
	void everyInstructionBelongsToOneAxis() {
		List<String> served = Arrays.stream(CareAxis.values()).map(CareAxisOptionsDTO::from)
				.flatMap(group -> group.options().stream()).map(CareAxisOptionsDTO.Option::name).toList();

		assertThat(served).doesNotHaveDuplicates().containsExactlyInAnyOrder(
				Arrays.stream(CareInstruction.values()).map(CareInstruction::name).toArray(String[]::new));
	}

	@Test
	@DisplayName("Every axis offers at least a positive and a negative choice")
	void everyAxisHasMoreThanOneOption() {
		// Um eixo com uma opção só é um campo que o admin não tem como responder:
		// ou marca a única instrução, ou deixa em branco.
		assertThat(Arrays.stream(CareAxis.values()).map(CareAxisOptionsDTO::from).toList())
				.allSatisfy(group -> assertThat(group.options()).as(group.label()).hasSizeGreaterThan(1));
	}
}
