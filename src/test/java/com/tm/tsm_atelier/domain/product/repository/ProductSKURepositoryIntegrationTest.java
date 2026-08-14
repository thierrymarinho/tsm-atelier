package com.tm.tsm_atelier.domain.product.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estes testes falavam com três consultas do repositório que existiam para
 * validar o {@code skuCode} vindo da request. O código passou a ser gerado, as
 * três saíram, e o que elas mediam por procuração — o que o
 * {@code @SQLRestriction} esconde e o que {@code idx_sku_code_active} reserva —
 * é medido aqui direto na fonte.
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
	@DisplayName("A soft-deleted SKU disappears from every entity query")
	void softDeletedSkuVanishesFromEntityQueries() {
		ProductSKU sku = anySku();
		Long skuId = sku.getId();
		String skuCode = sku.getSkuCode();

		softDelete(skuId);

		assertThat(skuRepository.findById(skuId)).isEmpty();
		assertThat(skuRepository.findByIdWithPessimisticLock(skuId)).isEmpty();
		assertThat(skuRepository.findBySkuCode(skuCode)).isEmpty();
	}

	/**
	 * O índice é parcial ({@code WHERE deleted_at IS NULL}), então a remoção
	 * devolve o código ao banco. Ninguém mais o reutiliza pela API — a sequência
	 * nunca repete um número —, mas é este comportamento que a restauração precisa
	 * enxergar para saber que um cadastro posterior pode ter tomado o lugar.
	 */
	@Test
	@DisplayName("A soft-deleted SKU frees its code in the partial index")
	void softDeletedSkuFreesItsCode() {
		ProductSKU sku = anySku();
		softDelete(sku.getId());

		assertThat(insertLiveSku(sku.getProductColor().getId(), sku.getSkuCode())).isEqualTo(1);
	}

	/**
	 * O outro lado da mesma regra, e a razão de o índice continuar existindo depois
	 * que o código deixou de ser digitado: o seed do V9 escreve direto na tabela,
	 * sem passar pelo ProductService. Se ele repetir um código, quem recusa é isto.
	 */
	@Test
	@DisplayName("A live SKU keeps its code reserved against a direct insert")
	void liveSkuKeepsItsCodeReserved() {
		ProductSKU sku = anySku();

		// Última instrução do teste de propósito: a violação aborta a transação no
		// Postgres, e qualquer consulta depois dela falharia por tabela.
		assertThatThrownBy(() -> insertLiveSku(sku.getProductColor().getId(), sku.getSkuCode()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private ProductSKU anySku() {
		return skuRepository.findAll().stream().findFirst().orElseThrow();
	}

	private void softDelete(Long skuId) {
		jdbcTemplate.update("UPDATE product_skus SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", skuId);
		entityManager.clear();
	}

	private int insertLiveSku(Long colorId, String skuCode) {
		return jdbcTemplate.update("INSERT INTO product_skus (product_color_id, size, sku_code) VALUES (?, 'M', ?)",
				colorId, skuCode);
	}
}
