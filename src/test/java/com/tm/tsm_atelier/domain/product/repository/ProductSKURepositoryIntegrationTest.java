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
	@DisplayName("A soft-deleted SKU disappears from entity queries but stays visible to the uniqueness check")
	void softDeletedSkuIsHiddenFromEntityQueriesButVisibleToUniquenessChecks() {
		ProductSKU sku = skuRepository.findAll().stream().findFirst().orElseThrow();
		Long skuId = sku.getId();
		String skuCode = sku.getSkuCode();

		jdbcTemplate.update("UPDATE product_skus SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", skuId);

		entityManager.clear();

		assertThat(skuRepository.findById(skuId)).isEmpty();
		assertThat(skuRepository.findByIdWithPessimisticLock(skuId)).isEmpty();

		assertThat(skuRepository.findExistingSkuCodes(List.of(skuCode))).containsExactly(skuCode);
		assertThat(skuRepository.existsBySkuCodeIncludingDeleted(skuCode)).isTrue();
		assertThat(skuRepository.existsBySkuCodeAndIdNot(skuCode, skuId + 1)).isTrue();
		assertThat(skuRepository.existsBySkuCodeAndIdNot(skuCode, skuId)).isFalse();
	}
}
