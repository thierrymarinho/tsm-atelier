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
	@DisplayName("Deve criar um produto com cores e SKUs com sucesso")
	void shouldCreateProductWithColorsAndSKUs() {
		// Arrange
		ProductRequestDTO requestDTO = aProductRequest().build();
		Product product = aProduct().build();
		Collection collection = aCollection().build();
		ProductColor savedColor = new ProductColor();
		ProductResponseDTO responseDTO = new ProductResponseDTO(1L, "Calça Jeans Skinny", "calca-jeans-skinny-1", null,
				null, null, null, null, null, null, true, false, null);

		when(productMapper.toEntity(any(ProductRequestDTO.class))).thenReturn(product);
		when(collectionRepository.findById(requestDTO.collectionId())).thenReturn(Optional.of(collection));
		when(productRepository.save(any(Product.class))).thenReturn(product);
		when(productMapper.toResponse(any(Product.class))).thenReturn(responseDTO);

		// Act
		ProductResponseDTO result = productService.create(requestDTO);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.name()).isEqualTo("Calça Jeans Skinny");

		verify(productMapper).toEntity(requestDTO);
		verify(collectionRepository).findById(requestDTO.collectionId());
		verify(productRepository, times(1)).save(any(Product.class));
		verify(productMapper).toResponse(product);
	}

	@Test
	@DisplayName("Deve lançar erro ao tentar criar produto em coleção inexistente")
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
	@DisplayName("Deve criar um produto sem coleção vinculada com sucesso")
	void shouldCreateProductWithoutCollectionSuccessfully() {
		// Arrange
		ProductRequestDTO requestDTO = aProductRequest().withCollectionId(null).build();
		Product product = aProduct().build();
		ProductColor savedColor = new ProductColor();
		ProductResponseDTO responseDTO = new ProductResponseDTO(1L, "Camiseta Básica", "camiseta-basica-1", null, null,
				null, null, null, null, null, true, false, null);

		when(productMapper.toEntity(any(ProductRequestDTO.class))).thenReturn(product);
		when(productRepository.save(any(Product.class))).thenReturn(product);
		when(productMapper.toResponse(any(Product.class))).thenReturn(responseDTO);

		// Act
		ProductResponseDTO result = productService.create(requestDTO);

		// Assert
		assertThat(result).isNotNull();
		verify(collectionRepository, never()).findById(anyLong()); // Garante que não buscou coleção
		verify(productRepository, times(1)).save(any(Product.class));
	}

	@Test
	@DisplayName("Deve buscar todos os produtos")
	void shouldFindAllProducts() {
		// Arrange
		Product product1 = aProduct().withId(1L).build();
		Product product2 = aProduct().withId(2L).build();
		when(productRepository.findAll()).thenReturn(java.util.List.of(product1, product2));
		when(productMapper.toResponse(any(Product.class))).thenReturn(
				new ProductResponseDTO(1L, "Prod 1", "prod-1", null, null, null, null, null, null, null, true, false,
						null),
				new ProductResponseDTO(2L, "Prod 2", "prod-2", null, null, null, null, null, null, null, true, false,
						null));

		// Act
		java.util.List<ProductResponseDTO> result = productService.findAllWithNPlusOne();

		// Assert
		assertThat(result).hasSize(2);
		verify(productRepository, times(1)).findAll();
	}

	@Test
	@DisplayName("Deve buscar um produto por ID com sucesso")
	void shouldFindProductByIdSuccessfully() {
		// Arrange
		Product product = aProduct().withId(1L).build();
		ProductResponseDTO responseDTO = new ProductResponseDTO(1L, "Produto Encontrado", "produto-encontrado-1", null,
				null, null, null, null, null, null, true, false, null);

		when(productRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(product));
		when(productMapper.toResponse(product)).thenReturn(responseDTO);

		// Act
		ProductResponseDTO result = productService.findById(1L);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.name()).isEqualTo("Produto Encontrado");
		verify(productRepository, times(1)).findByIdAndDeletedAtIsNull(1L);
	}

	@Test
	@DisplayName("Deve lançar erro ao buscar produto com ID inexistente")
	void shouldThrowErrorWhenProductNotFound() {
		// Arrange
		when(productRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> productService.findById(99L))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("Product not found with identifier: 99");
				
		verify(productMapper, never()).toResponse(any());
	}
}
