package com.tm.tsm_atelier.domain.product.service;

import static com.tm.tsm_atelier.common.builders.ProductRequestDTOBuilder.aProductRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.config.CacheNames;
import com.tm.tsm_atelier.domain.collection.dto.CollectionRequestDTO;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.collection.service.CollectionService;
import com.tm.tsm_atelier.domain.product.dto.AdminProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * A rota por slug carregava o produto por um id arrancado do último segmento do
 * texto. Como o gerador monta o slug a partir do nome, e só acrescenta número
 * quando há colisão, nada criado pela API era alcançável — e o que colidia era
 * alcançável errado.
 *
 * Precisa de banco: o que está sendo testado é a relação entre o slug que o
 * serviço grava e o que a consulta encontra, e um mock de repositório
 * concordaria com qualquer uma das duas versões.
 */
@SpringBootTest
@Transactional
@DisplayName("Catalog slug routing")
class CatalogSlugRoutingTest {

	@Autowired
	private ProductService productService;

	@Autowired
	private CollectionService collectionService;

	@Autowired
	private CacheManager cacheManager;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("A product is reachable by the slug its own creation returned")
	void createdProductIsReachableByItsSlug() {
		AdminProductResponseDTO created = productService.create(aProduct("Blusa Alcancavel"));
		reload();

		ProductResponseDTO found = productService.findBySlug(created.slug());

		assertThat(found.id()).isEqualTo(created.id());
	}

	/**
	 * O caso que servia conteúdo errado. O segundo produto de nome colidente recebe
	 * o sufixo -2, indistinguível de um id — e a rota antiga entregava o produto de
	 * id 2, com 301.
	 */
	@Test
	@DisplayName("Two products with colliding names each resolve to themselves")
	void collidingNamesResolveToThemselves() {
		AdminProductResponseDTO first = productService.create(aProduct("Blusa Disputada"));
		AdminProductResponseDTO second = productService.create(aProduct("Blusa Disputada Extra"));
		reload();

		assertThat(second.slug()).isNotEqualTo(first.slug());

		assertThat(productService.findBySlug(first.slug()).id()).isEqualTo(first.id());
		assertThat(productService.findBySlug(second.slug()).id()).as("o sufixo de desambiguação não é um id")
				.isEqualTo(second.id());
	}

	@Test
	@DisplayName("An unknown slug is a 404, not a redirect to whatever id it ends with")
	void unknownSlugIsNotFound() {
		assertThatThrownBy(() -> productService.findBySlug("qualquer-coisa-1"))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("A removed product disappears from the slug route")
	void removedProductIsNotReachableBySlug() {
		AdminProductResponseDTO created = productService.create(aProduct("Blusa Removida"));
		reload();

		productService.delete(created.id());
		reload();

		assertThatThrownBy(() -> productService.findBySlug(created.slug()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("Renaming a product keeps its slug, so the link never breaks")
	void renameKeepsTheSlug() {
		AdminProductResponseDTO created = productService.create(aProduct("Blusa Original"));
		reload();

		productService.update(created.id(), aProductRequest().withName("Blusa Rebatizada").withCollectionId(null)
				.withColors(created.colors().stream().map(colour -> aColour(colour.colorName())).toList()).build());
		reload();

		assertThat(productService.findBySlug(created.slug()).name()).isEqualTo("Blusa Rebatizada");
	}

	@Test
	@DisplayName("A collection is reachable by the slug its own creation returned")
	void createdCollectionIsReachableByItsSlug() {
		CollectionResponseDTO created = collectionService.create(aCollection("Colecao Alcancavel"));
		reload();

		assertThat(collectionService.findBySlug(created.slug()).id()).isEqualTo(created.id());
	}

	@Test
	@DisplayName("An unknown collection slug is a 404")
	void unknownCollectionSlugIsNotFound() {
		assertThatThrownBy(() -> collectionService.findBySlug("colecao-que-nao-existe"))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	/**
	 * Produto e coleção já dividiram o cache catalog_slug, desambiguados por um
	 * prefixo na chave. Hoje cada um tem o seu, porque a colisão de chave nunca foi
	 * o problema — ver aCollectionSurvivesARoundTripThroughItsCache().
	 */
	@Test
	@DisplayName("A product and a collection sharing a slug do not collide in the cache")
	void productAndCollectionWithTheSameSlugDoNotCollide() {
		AdminProductResponseDTO product = productService.create(aProduct("Nome Compartilhado"));
		CollectionResponseDTO collection = collectionService.create(aCollection("Nome Compartilhado"));
		reload();

		assertThat(product.slug()).isEqualTo(collection.slug());

		assertThat(productService.findBySlug(product.slug()).id()).isEqualTo(product.id());
		assertThat(collectionService.findBySlug(collection.slug()).id()).isEqualTo(collection.id());
	}

	/**
	 * O teste acima passava com o cache da coleção completamente quebrado, e é por
	 * isso que este existe.
	 *
	 * O defeito era o serviço apontar para um cache cujo serializer é
	 * ProductResponseDTO: a coleção era gravada sem reclamar e toda leitura
	 * quebrava ao forçar aquele JSON para dentro do tipo errado. Como o
	 * CacheErrorHandler trata falha de leitura como cache frio, a resposta
	 * continuava correta — vinda do banco, sempre, com um WARN por requisição.
	 *
	 * O que este caso prende é para qual cache o serviço escreve, e é a metade que
	 * importa: um teste que apenas gravasse um CollectionResponseDTO em
	 * catalog_slug_collection e o lesse de volta passaria com o serviço ainda
	 * apontando para o cache errado. Essa outra metade — a serialização em si —
	 * está em CatalogCacheSerializationTest#collectionDetailRoundTrips(), ao lado
	 * dos três caches vizinhos.
	 *
	 * A asserção é sobre o conteúdo do cache, e não sobre a resposta do serviço: a
	 * resposta estava certa o tempo todo. Lê-se direto do CacheManager porque é o
	 * caminho que não passa pelo tratador que engolia o erro — uma leitura com o
	 * serializer errado estoura aqui em vez de virar silêncio.
	 */
	@Test
	@DisplayName("The collection lookup caches into the collection cache")
	void aCollectionSurvivesARoundTripThroughItsCache() {
		CollectionResponseDTO created = collectionService.create(aCollection("Colecao Cacheada"));
		reload();

		CollectionResponseDTO served = collectionService.findBySlug(created.slug());

		Cache cache = cacheManager.getCache(CacheNames.CATALOG_SLUG_COLLECTION);
		assertThat(cache).as("o cache precisa existir — disableCreateOnMissingCache está ligado").isNotNull();

		// A escrita e a leitura não viajam pela mesma conexão, e o put do
		// @Cacheable retorna antes de o SET chegar ao servidor: sem esperar, a
		// leitura via um cache vazio de vez em quando. É a mesma corrida que
		// CatalogCacheSerializationTest.roundTrip trata, pelo mesmo motivo. Se o
		// valor nunca aparecer, o teste estoura no timeout em vez de passar.
		Cache.ValueWrapper cached = await().atMost(Duration.ofSeconds(5)).until(() -> cache.get(created.slug()),
				Objects::nonNull);

		assertThat(cached.get()).as("gravado e lido de volta, sem perder o tipo").isEqualTo(served);
	}

	private com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO aProduct(String name) {
		return aProductRequest().withName(name).withCollectionId(null).build();
	}

	private com.tm.tsm_atelier.domain.product.dto.ProductColorRequestDTO aColour(String name) {
		return new com.tm.tsm_atelier.domain.product.dto.ProductColorRequestDTO(null, name, "#0000FF",
				"http://cover.jpg", "http://hover.jpg", java.util.List.of(),
				java.util.List.of(new com.tm.tsm_atelier.domain.product.dto.ProductSKURequestDTO(null,
						com.tm.tsm_atelier.domain.product.enums.ProductSize.M, 5)));
	}

	private CollectionRequestDTO aCollection(String name) {
		return new CollectionRequestDTO(name, true, "Descrição", null, null, null, DisplayPosition.NONE, 0,
				TargetAudience.WOMEN);
	}

	private void reload() {
		entityManager.flush();
		entityManager.clear();
	}
}
