package com.tm.tsm_atelier.domain.collection.service;

import static com.tm.tsm_atelier.common.utils.SlugUtils.generateSlug;
import static com.tm.tsm_atelier.domain.collection.repository.CollectionSpecification.*;

import com.tm.tsm_atelier.common.exception.custom.EntityAlreadyExistsException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.domain.collection.dto.CollectionRequestDTO;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.collection.mapper.CollectionMapper;
import com.tm.tsm_atelier.domain.collection.repository.CollectionRepository;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollectionService {

	private final CollectionRepository collectionRepository;
	private final CollectionMapper collectionMapper;

	@Transactional
	public CollectionResponseDTO create(CollectionRequestDTO request) {
		validateUniqueConstraints(null, request);
		invalidateExistingHomeFeatured(null, request);

		Collection collection = collectionMapper.toEntity(request);
		collection = collectionRepository.save(collection);

		collection.setSlug(generateSlug(request.name()) + "-" + collection.getId());
		collection = collectionRepository.save(collection);

		return collectionMapper.toResponse(collection);
	}

	@Transactional(readOnly = true)
	public List<CollectionResponseDTO> findAll() {
		return collectionRepository.findAll().stream().map(collectionMapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<CollectionResponseDTO> findByFilters(DisplayPosition position, TargetAudience targetAudience) {
		Specification<Collection> spec = Specification.where(isActive()).and(hasPosition(position))
				.and(hasTargetAudience(targetAudience));

		return collectionRepository.findAll(spec, Sort.by("displayOrder").ascending()).stream()
				.map(collectionMapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public CollectionResponseDTO findById(Long id) {
		Collection collection = collectionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Collection", id));
		return collectionMapper.toResponse(collection);
	}

	@Transactional
	public CollectionResponseDTO update(Long id, CollectionRequestDTO request) {
		Collection collection = collectionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Collection", id));

		validateUniqueConstraints(id, request);
		invalidateExistingHomeFeatured(id, request);

		collectionMapper.updateEntityFromRequest(request, collection);
		collection.setSlug(generateSlug(request.name()) + "-" + collection.getId());

		return collectionMapper.toResponse(collection);
	}

	@Transactional
	public void delete(Long id) {
		Collection collection = collectionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Collection", id));
		collectionRepository.delete(collection);
	}

	private void invalidateExistingHomeFeatured(Long idToIgnore, CollectionRequestDTO request) {
		if (request.displayPosition() == DisplayPosition.HOME_FEATURED) {
			collectionRepository.findByDisplayPosition(DisplayPosition.HOME_FEATURED)
					.filter(existing -> idToIgnore == null || !existing.getId().equals(idToIgnore))
					.ifPresent(existing -> {
						existing.setDisplayPosition(DisplayPosition.NONE);
						collectionRepository.saveAndFlush(existing);
					});
		}
	}

	private void validateUniqueConstraints(Long id, CollectionRequestDTO request) {
		collectionRepository.findByNameAndTargetAudience(request.name(), request.targetAudience())
				.filter(existing -> id == null || !existing.getId().equals(id)).ifPresent(existing -> {
					throw new EntityAlreadyExistsException("Collection",
							request.name() + " para " + request.targetAudience());
				});

		if (request.displayPosition() == DisplayPosition.HEADER) {
			collectionRepository
					.findByDisplayPositionAndTargetAudience(DisplayPosition.HEADER, request.targetAudience())
					.filter(existing -> id == null || !existing.getId().equals(id)).ifPresent(existing -> {
						throw new EntityAlreadyExistsException("Collection", "HEADER para " + request.targetAudience());
					});
		}
	}
}
