package com.tm.tsm_atelier.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tm.tsm_atelier.common.dto.CustomPageImpl;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.dto.FabricCompositionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductColorResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSKUResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
// Sem isto o teste fica intermitente: o scheduler de expiracao roda de minuto
// em
// minuto dentro do contexto, cancela pedidos vencidos e o
// @CacheEvict(allEntries)
// de cancelAndRestoreStock apaga a entrada entre o put e o get.
@TestPropertySource(properties = "app.scheduler.order-expiration.enabled=false")
@DisplayName("Catalog cache serialization")
class CatalogCacheSerializationTest {

	@Autowired
	private CacheManager cacheManager;

	@Test
	@DisplayName("Product detail comes back from Redis with the color and SKU tree intact")
	void productDetailRoundTrips() {
		ProductResponseDTO stored = new ProductResponseDTO(1L, "Camisa Vestido", "camisa-vestido-1", "Descrição",
				List.of(new FabricCompositionResponseDTO("Algodão", 100)), List.of("Lavar à mão", "Não usar alvejante"),
				new BigDecimal("199.90"),
				new CollectionResponseDTO(2L, "Verão", "verao", "Descrição", true, "hero.jpg", "portrait.jpg",
						"square.jpg", DisplayPosition.HEADER, 1, TargetAudience.WOMEN),
				Category.DRESSES, TargetAudience.WOMEN, true, true,
				List.of(new ProductColorResponseDTO(3L, "Preto", "#000000", "cover.jpg", "hover.jpg",
						List.of("g1.jpg", "g2.jpg"),
						List.of(new ProductSKUResponseDTO(4L, ProductSize.M, "SKU-1", 7, null)), null)),
				null);

		ProductResponseDTO loaded = roundTrip(CacheNames.CATALOG_SLUG, "camisa-vestido-1", stored,
				ProductResponseDTO.class);

		assertThat(loaded).isEqualTo(stored);
		assertThat(loaded.colors().get(0).skus().get(0).stockQuantity()).isEqualTo(7);
		assertThat(loaded.collection().displayPosition()).isEqualTo(DisplayPosition.HEADER);
	}

	@Test
	@DisplayName("Catalog page comes back from Redis with pagination preserved")
	void catalogPageRoundTrips() {
		var stored = new CustomPageImpl<>(List.of(new ProductSummaryDTO(1L, "Camisa Vestido", "camisa-vestido-1",
				new BigDecimal("199.90"), true, "cover.jpg", "hover.jpg", List.of("#000000"), null, true)),
				PageRequest.of(0, 12), 37);

		Object key = List.of("termo", "DRESSES", 0, 12);
		@SuppressWarnings("unchecked")
		CustomPageImpl<ProductSummaryDTO> loaded = roundTrip(CacheNames.CATALOG_PRODUCTS, key, stored,
				CustomPageImpl.class);

		assertThat(loaded.getContent()).isEqualTo(stored.getContent());
		assertThat(loaded.getTotalElements()).isEqualTo(37);
		assertThat(loaded.getSize()).isEqualTo(12);
	}

	@Test
	@DisplayName("Collection list comes back from Redis in the order it was stored")
	void collectionListRoundTrips() {
		List<CollectionResponseDTO> stored = List.of(
				new CollectionResponseDTO(1L, "Verão", "verao", "d", true, "h.jpg", "p.jpg", "s.jpg",
						DisplayPosition.HEADER, 1, TargetAudience.WOMEN),
				new CollectionResponseDTO(2L, "Inverno", "inverno", "d", true, "h.jpg", "p.jpg", "s.jpg",
						DisplayPosition.HOME_MAIN, 2, TargetAudience.WOMEN));

		@SuppressWarnings("unchecked")
		List<CollectionResponseDTO> loaded = roundTrip(CacheNames.CATALOG_COLLECTIONS, List.of("HEADER", "WOMEN"),
				stored, List.class);

		assertThat(loaded).containsExactlyElementsOf(stored);
	}

	@Test
	@DisplayName("An undeclared cache name fails instead of being created without a type")
	void undeclaredCacheIsRejected() {
		assertThatThrownBy(() -> cacheManager.getCache("undeclared_cache").get("x"))
				.isInstanceOf(NullPointerException.class);
	}

	private <T> T roundTrip(String cacheName, Object key, Object value, Class<T> type) {
		Cache cache = cacheManager.getCache(cacheName);
		assertThat(cache).as("cache %s is not registered", cacheName).isNotNull();

		cache.evict(key);
		cache.put(key, value);

		Cache.ValueWrapper wrapper = cache.get(key);
		assertThat(wrapper).as("nothing came back from cache %s", cacheName).isNotNull();

		return type.cast(wrapper.get());
	}
}
