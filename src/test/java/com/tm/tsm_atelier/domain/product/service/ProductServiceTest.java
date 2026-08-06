package com.tm.tsm_atelier.domain.product.service;

import static com.tm.tsm_atelier.common.builders.CollectionBuilder.aCollection;
import static com.tm.tsm_atelier.common.builders.ProductBuilder.aProduct;
import static com.tm.tsm_atelier.common.builders.ProductRequestDTOBuilder.aProductRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.collection.repository.CollectionRepository;
import com.tm.tsm_atelier.domain.product.dto.FabricCompositionRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.entity.ProductColor;
import com.tm.tsm_atelier.domain.product.mapper.ProductMapper;
import com.tm.tsm_atelier.domain.product.repository.ProductColorRepository;
import com.tm.tsm_atelier.domain.product.repository.ProductRepository;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;
	@Mock
	private ProductColorRepository colorRepository;
	@Mock
	private ProductSKURepository skuRepository;
	@Mock
	private CollectionRepository collectionRepository;
	@Mock
	private ProductMapper productMapper;

	@InjectMocks
	private ProductService productService;

	@Test
	@DisplayName("Should create a product with colors and SKUs successfully")
	void shouldCreateProductWithColorsAndSKUs() {
		// Arrange
		ProductRequestDTO requestDTO = aProductRequest().build();
		Product product = aProduct().build();
		Collection collection = aCollection().build();
		ProductColor savedColor = new ProductColor();
		ProductResponseDTO responseDTO = new ProductResponseDTO(1L, "Calça Jeans Skinny", "calca-jeans-skinny-1", null,
				null, null, null, null, null, null, null, true, false, null, null);

		when(productMapper.toEntity(any(ProductRequestDTO.class))).thenReturn(product);
		when(collectionRepository.findById(requestDTO.collectionId())).thenReturn(Optional.of(collection));
		when(productRepository.save(any(Product.class))).thenReturn(product);
		when(productMapper.toAdminResponse(any(Product.class))).thenReturn(responseDTO);

		// Act
		ProductResponseDTO result = productService.create(requestDTO);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.name()).isEqualTo("Calça Jeans Skinny");

		verify(productMapper).toEntity(requestDTO);
		verify(collectionRepository).findById(requestDTO.collectionId());
		verify(productRepository, times(1)).save(any(Product.class));
		verify(productMapper).toAdminResponse(product);
	}

	@Test
	@DisplayName("Should throw when creating a product in a collection that does not exist")
	void shouldThrowErrorWhenCollectionNotFound() {
		// Arrange
		ProductRequestDTO requestDTO = aProductRequest().withCollectionId(999L).build();
		Product product = aProduct().build();

		when(productMapper.toEntity(any(ProductRequestDTO.class))).thenReturn(product);
		when(collectionRepository.findById(999L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> productService.create(requestDTO))
				.isInstanceOf(com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException.class)
				.hasMessageContaining("Collection not found with identifier: 999");

		verify(productRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should create a product with no linked collection successfully")
	void shouldCreateProductWithoutCollectionSuccessfully() {
		// Arrange
		ProductRequestDTO requestDTO = aProductRequest().withCollectionId(null).build();
		Product product = aProduct().build();
		ProductColor savedColor = new ProductColor();
		ProductResponseDTO responseDTO = new ProductResponseDTO(1L, "Camiseta Básica", "camiseta-basica-1", null, null,
				null, null, null, null, null, null, true, false, null, null);

		when(productMapper.toEntity(any(ProductRequestDTO.class))).thenReturn(product);
		when(productRepository.save(any(Product.class))).thenReturn(product);
		when(productMapper.toAdminResponse(any(Product.class))).thenReturn(responseDTO);

		// Act
		ProductResponseDTO result = productService.create(requestDTO);

		// Assert
		assertThat(result).isNotNull();
		verify(collectionRepository, never()).findById(anyLong()); // Garante que não buscou coleção
		verify(productRepository, times(1)).save(any(Product.class));
	}

	@Test
	@DisplayName("Should fetch a product by id successfully")
	void shouldFindProductByIdSuccessfully() {
		// Arrange
		Product product = aProduct().withId(1L).build();
		ProductResponseDTO responseDTO = new ProductResponseDTO(1L, "Produto Encontrado", "produto-encontrado-1", null,
				null, null, null, null, null, null, null, true, false, null, null);

		when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));
		when(productMapper.toCatalogResponse(product)).thenReturn(responseDTO);

		// Act
		ProductResponseDTO result = productService.findById(1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.name()).isEqualTo("Produto Encontrado");
		verify(productRepository, times(1)).findByIdAndDeletedAtIsNull(1L);
	}

	@Test
	@DisplayName("Should throw when fetching a product with an id that does not exist")
	void shouldThrowErrorWhenProductNotFound() {
		// Arrange
		when(productRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> productService.findById(99L))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("Product not found with identifier: 99");
				
		verify(productMapper, never()).toCatalogResponse(any());
	}

	@Test
	@DisplayName("Should reject a promotional price that is not lower than the regular price")
	void shouldRejectPromotionalPriceNotLowerThanPrice() {
		// A mesma regra existe como CHECK constraint na migration V3. Aqui ela vale
		// pela mensagem: sem esta validacao o admin receberia uma violacao de
		// integridade do banco, sem saber qual campo corrigir.
		ProductRequestDTO request = aProductRequest().withPrice(new java.math.BigDecimal("100.00"))
				.withPromotionalPrice(new java.math.BigDecimal("100.00")).build();

		assertThatThrownBy(() -> productService.create(request)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("must be lower than the regular price");

		verify(productRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should reject a fabric composition that repeats the same material")
	void shouldRejectDuplicateFabricMaterials() {
		// A PK (product_id, material) rejeitaria isso no banco, mas como um 409
		// "A data conflict occurred" que nao diz qual material esta repetido. E os
		// dois 50% somam 100%, entao a validacao de percentual deixava passar.
		ProductRequestDTO request = aProductRequest().withFabricCompositions(java.util.List
				.of(new FabricCompositionRequestDTO("Algodao", 50), new FabricCompositionRequestDTO("Algodao", 50)))
				.build();

		assertThatThrownBy(() -> productService.create(request)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("cannot repeat the same material").hasMessageContaining("Algodao");

		verify(productRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should accept a fabric composition with distinct materials")
	void shouldAcceptDistinctFabricMaterials() {
		ProductRequestDTO request = aProductRequest().withCollectionId(null).withFabricCompositions(java.util.List
				.of(new FabricCompositionRequestDTO("Algodao", 60), new FabricCompositionRequestDTO("Elastano", 40)))
				.build();
		Product product = aProduct().build();

		when(productMapper.toEntity(any(ProductRequestDTO.class))).thenReturn(product);
		when(productRepository.save(any(Product.class))).thenReturn(product);

		productService.create(request);

		verify(productRepository).save(any(Product.class));
	}

	@Test
	@DisplayName("Should accept a promotional price below the regular price")
	void shouldAcceptPromotionalPriceBelowPrice() {
		ProductRequestDTO request = aProductRequest().withCollectionId(null)
				.withPrice(new java.math.BigDecimal("100.00")).withPromotionalPrice(new java.math.BigDecimal("79.90"))
				.build();
		Product product = aProduct().withPromotionalPrice(new java.math.BigDecimal("79.90")).build();

		when(productMapper.toEntity(any(ProductRequestDTO.class))).thenReturn(product);
		when(productRepository.save(any(Product.class))).thenReturn(product);

		productService.create(request);

		verify(productRepository).save(any(Product.class));
	}
}
