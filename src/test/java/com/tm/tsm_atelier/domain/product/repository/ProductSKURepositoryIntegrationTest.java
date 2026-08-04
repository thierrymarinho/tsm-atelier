package com.tm.tsm_atelier.domain.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cobre a assimetria criada pelo soft-delete de SKUs: o @SQLRestriction precisa
 * esconder a linha das consultas de entidade (senão um produto retirado de
 * venda continua comprável), enquanto as checagens de duplicidade precisam
 * continuar enxergando a mesma linha, porque o índice único de sku_code no
 * banco não ignora registros deletados.
 */
@SpringBootTest
@Transactional
class ProductSKURepositoryIntegrationTest {

	@Autowired
	private ProductSKURepository skuRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("SKU soft-deleted some das consultas de entidade mas segue visível para a checagem de duplicidade")
	void softDeletedSkuIsHiddenFromEntityQueriesButVisibleToUniquenessChecks() {
		ProductSKU sku = skuRepository.findAll().stream().findFirst().orElseThrow();
		Long skuId = sku.getId();
		String skuCode = sku.getSkuCode();

		jdbcTemplate.update("UPDATE product_skus SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", skuId);

		// O @SQLRestriction age no SQL; sem limpar o contexto de persistência o
		// findById devolveria a instância já carregada pelo cache de 1º nível.
		entityManager.clear();

		assertThat(skuRepository.findById(skuId)).isEmpty();
		assertThat(skuRepository.findByIdWithPessimisticLock(skuId)).isEmpty();

		assertThat(skuRepository.findExistingSkuCodes(List.of(skuCode))).containsExactly(skuCode);
		assertThat(skuRepository.existsBySkuCodeIncludingDeleted(skuCode)).isTrue();
		assertThat(skuRepository.existsBySkuCodeAndIdNot(skuCode, skuId + 1)).isTrue();
		assertThat(skuRepository.existsBySkuCodeAndIdNot(skuCode, skuId)).isFalse();
	}
}
