package com.tm.tsm_atelier.domain.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tm.tsm_atelier.domain.admin.service.AuditService;
import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.collection.mapper.CollectionMapper;
import com.tm.tsm_atelier.domain.collection.repository.CollectionRepository;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionService.delete()")
class CollectionDeleteTest {

	@Mock
	private CollectionRepository collectionRepository;

	@Mock
	private CollectionMapper collectionMapper;

	@Mock
	private ProductService productService;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private CollectionService collectionService;

	/**
	 * A cascata era o comportamento único e não anunciado: excluir uma coleção
	 * apagava todo o catálogo dela, e nada na rota dizia isso.
	 */
	@Test
	@DisplayName("Should detach the products by default instead of deleting them")
	void shouldDetachProductsByDefault() {
		Product product = aProduct(1L, null);
		Collection collection = aCollectionWith(product);

		when(collectionRepository.findById(10L)).thenReturn(Optional.of(collection));

		collectionService.delete(10L, false);

		verify(productService, never()).delete(1L);
		assertThat(product.getCollection()).isNull();
		assertThat(product.getDeletedAt()).isNull();
		verify(collectionRepository).delete(collection);
	}

	@Test
	@DisplayName("Should delete the products when the caller asks for the cascade")
	void shouldCascadeWhenRequested() {
		Product product = aProduct(1L, null);
		Collection collection = aCollectionWith(product);

		when(collectionRepository.findById(10L)).thenReturn(Optional.of(collection));

		collectionService.delete(10L, true);

		verify(productService).delete(1L);
	}

	/**
	 * Product não tem @SQLRestriction, então produtos já removidos continuavam
	 * chegando na lista. ProductService.delete filtra por deletedAt IS NULL, não os
	 * encontrava, e a exclusão da coleção inteira voltava como 404 "Product not
	 * found" — deixando permanentemente inexcluível qualquer coleção que já tivesse
	 * perdido um produto.
	 */
	@Test
	@DisplayName("Should ignore products that were already deleted")
	void shouldIgnoreAlreadyDeletedProducts() {
		Product living = aProduct(1L, null);
		Product alreadyDeleted = aProduct(2L, LocalDateTime.now().minusDays(1));
		Collection collection = aCollectionWith(living, alreadyDeleted);

		when(collectionRepository.findById(10L)).thenReturn(Optional.of(collection));

		assertThatCode(() -> collectionService.delete(10L, true)).doesNotThrowAnyException();

		verify(productService).delete(1L);
		verify(productService, never()).delete(2L);
	}

	private Product aProduct(Long id, LocalDateTime deletedAt) {
		return Product.builder().id(id).name("Product " + id).deletedAt(deletedAt).build();
	}

	private Collection aCollectionWith(Product... products) {
		return Collection.builder().id(10L).name("Verão 26").products(List.of(products)).build();
	}
}
