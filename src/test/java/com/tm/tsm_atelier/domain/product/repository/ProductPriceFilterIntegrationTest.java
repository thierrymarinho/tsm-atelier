package com.tm.tsm_atelier.domain.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.domain.product.entity.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * O filtro roda contra o banco de verdade porque o que está sendo verificado é
 * a tradução do COALESCE para SQL, não a lógica Java. A migration V10 cria um
 * índice funcional sobre a mesma expressão — se uma mudar sem a outra, a busca
 * continua correta mas passa a varrer a tabela.
 */
@SpringBootTest
@Transactional
@DisplayName("Price range filter with promotional price")
class ProductPriceFilterIntegrationTest {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("Should find a product whose promotional price falls in range even when its list price does not")
	void shouldFindByPromotionalPrice() {
		Long onSaleId = insertProduct("promo-em-faixa", new BigDecimal("200.00"), new BigDecimal("90.00"));
		Long fullPriceId = insertProduct("sem-promo", new BigDecimal("200.00"), null);

		var found = productRepository.findAll(upTo(new BigDecimal("100.00")));

		// O de R$ 200 em promocao por R$ 90 entra numa busca "ate R$ 100"; sem o
		// COALESCE ele sumiria da vitrine justamente por estar mais barato.
		assertThat(found).extracting(Product::getId).contains(onSaleId).doesNotContain(fullPriceId);
	}

	@Test
	@DisplayName("Should drop a product whose list price is in range but whose promotional price is not")
	void shouldRespectThePromotionalPriceOnTheLowerBound() {
		Long id = insertProduct("promo-abaixo-da-faixa", new BigDecimal("120.00"), new BigDecimal("80.00"));

		var found = productRepository.findAll(ProductSpecification.priceBetween(new BigDecimal("100.00"), null));

		// O preco de tabela (120) entraria na faixa, mas o cliente paga 80.
		assertThat(found).extracting(Product::getId).doesNotContain(id);
	}

	private Specification<Product> upTo(BigDecimal maxPrice) {
		return ProductSpecification.priceBetween(null, maxPrice);
	}

	private Long insertProduct(String slug, BigDecimal price, BigDecimal promotionalPrice) {
		return jdbcTemplate.queryForObject(
				"INSERT INTO products (name, slug, price, promotional_price, category, target_audience, active, is_featured) "
						+ "VALUES (?, ?, ?, ?, 'T_SHIRTS', 'MEN', true, false) RETURNING id",
				Long.class, "Produto " + slug, slug, price, promotionalPrice);
	}
}
