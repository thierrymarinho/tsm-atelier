package com.tm.tsm_atelier.domain.collection.service;

import static com.tm.tsm_atelier.common.builders.CollectionBuilder.aCollection;
import static com.tm.tsm_atelier.common.builders.CollectionRequestDTOBuilder.aCollectionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.tm.tsm_atelier.domain.collection.dto.CollectionRequestDTO;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.collection.mapper.CollectionMapper;
import com.tm.tsm_atelier.domain.collection.repository.CollectionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

	@Mock
	private CollectionRepository collectionRepository;

	@Mock
	private CollectionMapper collectionMapper;

	@InjectMocks
	private CollectionService collectionService;

	@Test
	@DisplayName("Deve criar uma coleção com sucesso")
	void shouldCreateCollectionSuccessfully() {
		// Arrange
		CollectionRequestDTO requestDTO = aCollectionRequest().build();
		Collection collection = aCollection().build();

		CollectionResponseDTO responseDTO = new CollectionResponseDTO(1L, requestDTO.name(), "dummy-slug-1",
				requestDTO.description(), requestDTO.active(), requestDTO.imageUrl(), requestDTO.displayPosition(),
				requestDTO.displayOrder(), requestDTO.targetAudience());

		when(collectionRepository.findByNameAndTargetAudience(requestDTO.name(), requestDTO.targetAudience()))
				.thenReturn(java.util.Optional.empty());
		when(collectionMapper.toEntity(any(CollectionRequestDTO.class))).thenReturn(collection);
		when(collectionRepository.save(any(Collection.class))).thenReturn(collection);
		when(collectionMapper.toResponse(any(Collection.class))).thenReturn(responseDTO);
		// Act
		CollectionResponseDTO result = collectionService.create(requestDTO);
		// Assert
		assertThat(result).isNotNull();
		assertThat(result.name()).isEqualTo(requestDTO.name());
		assertThat(result.targetAudience()).isEqualTo(requestDTO.targetAudience());
		assertThat(result.id()).isEqualTo(1L);

		verify(collectionRepository, times(2)).save(any(Collection.class));
		verify(collectionMapper, times(1)).toEntity(any(CollectionRequestDTO.class));
		verify(collectionRepository, times(1)).findByNameAndTargetAudience(requestDTO.name(),
				requestDTO.targetAudience());
	}

	@Test
	@DisplayName("Deve buscar todas as coleções")
	void shouldFindAllCollections() {
		// Arrange

		// Act

		// Assert
	}
}
