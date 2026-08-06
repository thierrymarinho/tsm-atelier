package com.tm.tsm_atelier.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.config.CacheNames;
import com.tm.tsm_atelier.domain.product.dto.ProductSearchFilter;
import com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * O teste nao roda em transacao com rollback de proposito. O catalogo e
 * cacheado no Redis, que nao participa do rollback — dados inseridos numa
 * transacao desfeita ficariam cacheados apontando para produtos que deixaram de
 * existir, envenenando as execucoes seguintes. Por isso a limpeza e explicita.
 */
@SpringBootTest
@DisplayName("On-sale filter in the catalog search")
class ProductOnSaleFilterTest {

	private static final String MARKER = "ZZOnSaleFilterFixture";

	@Autowired
	private ProductService productService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CacheManager cacheManager;

	@BeforeEach
	void setUp() {
		cleanUp();
		insertProduct("zz-on-sale-fixture", new BigDecimal("200.00"), new BigDecimal("150.00"));
		insertProduct("zz-full-price-fixture", new BigDecimal("120.00"), null);
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	@Test
	@DisplayName("Should return only products that have a promotional price when onSale is true")
	void shouldReturnOnlyProductsOnSale() {
		Page<ProductSummaryDTO> result = search(true);

		assertThat(result).extracting(ProductSummaryDTO::slug).containsExactly("zz-on-sale-fixture");
	}

	@Test
	@DisplayName("Should return only products without a promotional price when onSale is false")
	void shouldReturnOnlyProductsNotOnSale() {
		Page<ProductSummaryDTO> result = search(false);

		assertThat(result).extracting(ProductSummaryDTO::slug).containsExactly("zz-full-price-fixture");
	}

	@Test
	@DisplayName("Should return every product when onSale is omitted")
	void shouldNotFilterWhenOnSaleIsOmitted() {
		Page<ProductSummaryDTO> result = search(null);

		assertThat(result).extracting(ProductSummaryDTO::slug).containsExactlyInAnyOrder("zz-on-sale-fixture",
				"zz-full-price-fixture");
	}

	/**
	 * A chave do cache de searchCatalog lista os parametros um a um. Um parametro
	 * novo que fique de fora da chave faz duas buscas diferentes compartilharem a
	 * mesma entrada: a vitrine de promocoes passa a servir o catalogo inteiro, ou o
	 * contrario, dependendo de quem chegou primeiro. Falha so em producao, com
	 * cache quente. Este teste aquece o cache com onSale=null e verifica que a
	 * busca seguinte nao herda o resultado.
	 */
	@Test
	@DisplayName("Should keep onSale in the cache key so a warm cache does not leak across filters")
	void shouldNotShareTheCacheEntryBetweenDifferentOnSaleValues() {
		Page<ProductSummaryDTO> unfiltered = search(null);
		assertThat(unfiltered).hasSize(2);

		Page<ProductSummaryDTO> onSale = search(true);

		assertThat(onSale).extracting(ProductSummaryDTO::slug).containsExactly("zz-on-sale-fixture");
	}

	private Page<ProductSummaryDTO> search(Boolean onSale) {
		ProductSearchFilter filter = new ProductSearchFilter(MARKER, null, null, null, null, null, null, onSale);
		return productService.searchCatalog(filter, PageRequest.of(0, 20));
	}

	private void insertProduct(String slug, BigDecimal price, BigDecimal promotionalPrice) {
		jdbcTemplate.update(
				"INSERT INTO products (name, slug, price, promotional_price, category, target_audience, active, is_featured) "
						+ "VALUES (?, ?, ?, ?, 'T_SHIRTS', 'MEN', true, false)",
				MARKER + " " + slug, slug, price, promotionalPrice);
	}

	private void cleanUp() {
		jdbcTemplate.update("DELETE FROM products WHERE name LIKE ?", MARKER + "%");

		Cache cache = cacheManager.getCache(CacheNames.CATALOG_PRODUCTS);
		if (cache != null) {
			cache.clear();
		}
	}
}
