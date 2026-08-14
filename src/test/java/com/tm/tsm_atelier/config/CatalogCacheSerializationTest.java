package com.tm.tsm_atelier.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.tm.tsm_atelier.common.dto.CustomPageImpl;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.product.dto.CareInstructionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.FabricCompositionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductColorResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSKUResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO;
import com.tm.tsm_atelier.domain.product.enums.CareInstruction;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.Material;
import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "app.scheduler.order-expiration.enabled=false")
@DisplayName("Catalog cache serialization")
class CatalogCacheSerializationTest {

	@Autowired
	private CacheManager cacheManager;

	@Test
	@DisplayName("Product detail comes back from Redis with the color and SKU tree intact")
	void productDetailRoundTrips() {
		ProductResponseDTO stored = new ProductResponseDTO(1L, "Camisa Vestido", "camisa-vestido-1", "Descrição",
				List.of(new FabricCompositionResponseDTO(Material.COTTON, "Algodão", 100)),
				List.of(CareInstructionResponseDTO.from(CareInstruction.HAND_WASH),
						CareInstructionResponseDTO.from(CareInstruction.DO_NOT_BLEACH)),
				new BigDecimal("199.90"), new BigDecimal("149.90"),
				new CollectionResponseDTO(2L, "Verão", "verao", "Descrição", true, "hero.jpg", "portrait.jpg",
						"square.jpg", DisplayPosition.HEADER, 1, TargetAudience.WOMEN),
				Category.DRESSES, TargetAudience.WOMEN, true, true,
				List.of(new ProductColorResponseDTO(3L, "Preto", "#000000", "cover.jpg", "hover.jpg",
						List.of("g1.jpg", "g2.jpg"),
						List.of(new ProductSKUResponseDTO(4L, ProductSize.M, "SKU-1", 7)))));

		ProductResponseDTO loaded = roundTrip(CacheNames.CATALOG_SLUG, "camisa-vestido-1", stored,
				ProductResponseDTO.class);

		assertThat(loaded).isEqualTo(stored);
		assertThat(loaded.colors().get(0).skus().get(0).stockQuantity()).isEqualTo(7);
		assertThat(loaded.collection().displayPosition()).isEqualTo(DisplayPosition.HEADER);
	}

	/**
	 * A coleção tem cache próprio, e não uma chave prefixada dentro de
	 * {@code catalog_slug}, porque um cache do Redis tem <strong>um</strong>
	 * serializer de valor por nome. Enquanto os dois dividiram o mesmo cache, o
	 * serializer era o do produto: a coleção era gravada sem reclamar e toda
	 * leitura quebrava ao forçar aquele JSON para dentro de
	 * {@code ProductResponseDTO}.
	 *
	 * <p>
	 * Este caso cobre a metade de serialização, como os três vizinhos. Que o
	 * serviço escreva <em>neste</em> cache e não no outro é a outra metade, e vive
	 * em {@code CatalogSlugRoutingTest} — nenhuma das duas pega o defeito sozinha.
	 */
	@Test
	@DisplayName("Collection detail comes back from Redis as a collection, not as a product")
	void collectionDetailRoundTrips() {
		CollectionResponseDTO stored = new CollectionResponseDTO(2L, "Flora", "flora", "Descrição", true, "hero.jpg",
				"portrait.jpg", "square.jpg", DisplayPosition.HOME_MAIN, 3, TargetAudience.WOMEN);

		CollectionResponseDTO loaded = roundTrip(CacheNames.CATALOG_SLUG_COLLECTION, "flora", stored,
				CollectionResponseDTO.class);

		assertThat(loaded).isEqualTo(stored);
		assertThat(loaded.displayPosition()).isEqualTo(DisplayPosition.HOME_MAIN);
	}

	@Test
	@DisplayName("Catalog page comes back from Redis with pagination preserved")
	void catalogPageRoundTrips() {
		var stored = new CustomPageImpl<>(
				List.of(new ProductSummaryDTO(1L, "Camisa Vestido", "camisa-vestido-1", new BigDecimal("199.90"),
						new BigDecimal("149.90"), true, "cover.jpg", "hover.jpg", List.of("#000000"), true)),
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

		// A escrita e a leitura nao viajam pela mesma conexao: o RedisCache escreve
		// por um socket e le por outro, e o put retorna antes de o SET ser executado
		// no servidor. Sem esperar, o GET chegava ate 0,1 ms antes do SET e o teste
		// via um cache vazio. O alvo aqui e a serializacao dos DTOs, nao o modelo de
		// consistencia do Redis, entao a espera remove a corrida sem esconder falha:
		// se o valor nunca aparecer, o teste estoura no timeout.
		Cache.ValueWrapper wrapper = await().atMost(Duration.ofSeconds(5)).until(() -> cache.get(key),
				Objects::nonNull);

		return type.cast(wrapper.get());
	}
}
