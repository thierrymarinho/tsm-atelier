package com.tm.tsm_atelier.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.domain.product.enums.CareAxis;
import com.tm.tsm_atelier.domain.product.enums.CareInstruction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * O seed do V9 escreve direto na tabela, e esse caminho não passa pelo
 * {@code ProductService} — nenhuma das duas regras de cuidado é imposta pelo
 * banco, porque o eixo não é coluna. Estes testes são o que resta no lugar
 * delas, e valem para qualquer seed futuro, não só para o de hoje.
 */
@SpringBootTest
@Transactional
@DisplayName("Seeded care instructions")
class CareInstructionSeedTest {

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("No seeded product answers the same axis twice")
	void noProductContradictsItself() {
		Map<Long, List<CareInstruction>> byProduct = seededInstructions();

		assertThat(byProduct).isNotEmpty();
		assertThat(byProduct).allSatisfy((productId, instructions) -> {
			List<CareAxis> axes = instructions.stream().map(CareInstruction::getAxis).toList();
			assertThat(axes).as("produto %d: %s", productId, instructions).doesNotHaveDuplicates();
		});
	}

	/**
	 * O {@code JOIN} do seed é por fibra dominante: um produto de um material sem
	 * regra correspondente simplesmente não entra, e a falta de etiqueta só
	 * apareceria na vitrine. Aqui ela quebra a build.
	 */
	@Test
	@DisplayName("Every seeded product got a care label")
	void everyProductHasInstructions() {
		List<Long> withoutCare = entityManager.createNativeQuery("""
				SELECT p.id FROM products p
				WHERE NOT EXISTS (SELECT 1 FROM product_care_instructions c WHERE c.product_id = p.id)
				""", Long.class).getResultList();

		assertThat(withoutCare).as("produtos sem nenhuma instrução de cuidado").isEmpty();
	}

	@SuppressWarnings("unchecked")
	private Map<Long, List<CareInstruction>> seededInstructions() {
		List<Object[]> rows = entityManager
				.createNativeQuery("SELECT product_id, instruction FROM product_care_instructions").getResultList();

		return rows.stream().collect(Collectors.groupingBy(row -> ((Number) row[0]).longValue(),
				Collectors.mapping(row -> CareInstruction.valueOf((String) row[1]), Collectors.toList())));
	}
}
