package com.tm.tsm_atelier.domain.collection.service;

import static com.tm.tsm_atelier.common.utils.SlugUtils.generateSlug;
import static com.tm.tsm_atelier.domain.collection.repository.CollectionSpecification.*;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.common.exception.custom.EntityAlreadyExistsException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.config.CacheNames;
import com.tm.tsm_atelier.domain.admin.entity.AuditAction;
import com.tm.tsm_atelier.domain.admin.entity.AuditedEntity;
import com.tm.tsm_atelier.domain.admin.service.AuditService;
import com.tm.tsm_atelier.domain.collection.dto.CollectionRequestDTO;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.collection.entity.Collection;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.collection.mapper.CollectionMapper;
import com.tm.tsm_atelier.domain.collection.repository.CollectionRepository;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollectionService {

	private final CollectionRepository collectionRepository;
	private final CollectionMapper collectionMapper;
	private final ProductService productService;
	private final AuditService auditService;

	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_COLLECTIONS, CacheNames.CATALOG_SLUG_COLLECTION, CacheNames.CATALOG_SLUG,
			CacheNames.CATALOG_PRODUCTS}, allEntries = true)
	public CollectionResponseDTO create(CollectionRequestDTO request) {
		validateUniqueConstraints(null, request);
		invalidateExistingDisplayPositions(null, request);

		Collection collection = collectionMapper.toEntity(request);
		collection.setSlug(generateUniqueSlug(request.name()));

		collection = collectionRepository.save(collection);

		auditService.record(AuditedEntity.COLLECTION, collection.getId(), AuditAction.CREATED);

		return collectionMapper.toResponse(collection);
	}

	@Transactional(readOnly = true)
	public List<CollectionResponseDTO> findAll() {
		return collectionRepository.findAll().stream().map(collectionMapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	@Cacheable(value = CacheNames.CATALOG_COLLECTIONS, key = "{#position, #targetAudience}")
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

	@Transactional(readOnly = true)
	@Cacheable(value = CacheNames.CATALOG_SLUG_COLLECTION, key = "#slug")
	public CollectionResponseDTO findBySlug(String slug) {
		Collection collection = collectionRepository.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Collection", slug));
		return collectionMapper.toResponse(collection);
	}

	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_COLLECTIONS, CacheNames.CATALOG_SLUG_COLLECTION, CacheNames.CATALOG_SLUG,
			CacheNames.CATALOG_PRODUCTS}, allEntries = true)
	public CollectionResponseDTO update(Long id, CollectionRequestDTO request) {
		Collection collection = collectionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Collection", id));

		validateUniqueConstraints(id, request);
		invalidateExistingDisplayPositions(id, request);

		collectionMapper.updateEntityFromRequest(request, collection);
		if (collection.getSlug() == null) {
			collection.setSlug(generateUniqueSlug(request.name()));
		}

		auditService.record(AuditedEntity.COLLECTION, id, AuditAction.UPDATED);

		return collectionMapper.toResponse(collection);
	}

	/**
	 * Excluir a coleção desassocia os produtos por padrão; passar cascadeProducts
	 * exclui cada um deles junto.
	 */
	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_COLLECTIONS, CacheNames.CATALOG_SLUG_COLLECTION, CacheNames.CATALOG_SLUG,
			CacheNames.CATALOG_PRODUCTS}, allEntries = true)
	public void delete(Long id, boolean cascadeProducts) {
		Collection collection = collectionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Collection", id));

		List<Product> livingProducts = collection.getProducts().stream().filter(p -> p.getDeletedAt() == null).toList();

		if (cascadeProducts) {
			livingProducts.forEach(product -> productService.delete(product.getId()));
		} else {
			livingProducts.forEach(product -> product.setCollection(null));
		}

		auditService.record(AuditedEntity.COLLECTION, id, AuditAction.DELETED,
				livingProducts.size() + " products " + (cascadeProducts ? "deleted" : "detached"));

		collection.setDeletedAt(LocalDateTime.now());
		collection.setActive(false);
		collectionRepository.delete(collection);
	}

	/**
	 * Restaura a coleção, e só ela.
	 *
	 * Ela volta inativa e com displayPosition NONE. Os índices parciais do V2
	 * liberam a posição de destaque na exclusão — para que uma coleção no lixo não
	 * bloqueie o HOME_MAIN do site inteiro — então outra pode tê-la ocupado no
	 * intervalo, e devolvê-la ocupada quebraria no índice.
	 *
	 * Nome e slug são o contrário: as constraints são totais, a coleção removida
	 * nunca os soltou, e por isso a restauração nunca falha por conflito deles.
	 *
	 * Os produtos não voltam junto. Na exclusão padrão eles foram desassociados e o
	 * vínculo não existe mais; na exclusão em cascata cada um tem a própria rota,
	 * com as próprias checagens de código de SKU.
	 */
	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_COLLECTIONS, CacheNames.CATALOG_SLUG_COLLECTION, CacheNames.CATALOG_SLUG,
			CacheNames.CATALOG_PRODUCTS}, allEntries = true)
	public CollectionResponseDTO restore(Long id) {
		Collection deleted = collectionRepository.findByIdIncludingDeleted(id)
				.orElseThrow(() -> new ResourceNotFoundException("Collection", id));

		if (deleted.getDeletedAt() == null) {
			throw new BusinessRuleException("Collection " + id + " is not deleted.");
		}

		collectionRepository.restoreCollection(id);

		Collection restored = collectionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Collection", id));

		restored.setActive(false);

		long orphanedProducts = restored.getProducts().stream().filter(p -> p.getDeletedAt() != null).count();
		auditService.record(AuditedEntity.COLLECTION, id, AuditAction.RESTORED,
				orphanedProducts + " products still deleted");

		return collectionMapper.toResponse(restored);
	}

	private void invalidateExistingDisplayPositions(Long idToIgnore, CollectionRequestDTO request) {
		if (request.displayPosition() == DisplayPosition.HOME_MAIN) {
			collectionRepository.findByDisplayPosition(DisplayPosition.HOME_MAIN)
					.filter(existing -> idToIgnore == null || !existing.getId().equals(idToIgnore))
					.ifPresent(existing -> {
						existing.setDisplayPosition(DisplayPosition.NONE);
						collectionRepository.saveAndFlush(existing);
					});
		} else if (request.displayPosition() == DisplayPosition.HOME_SECONDARY) {
			collectionRepository
					.findByDisplayPositionAndTargetAudience(DisplayPosition.HOME_SECONDARY, request.targetAudience())
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
							request.name() + " for " + request.targetAudience());
				});

		collectionRepository.findDeletedIdByNameAndTargetAudience(request.name(), request.targetAudience().name())
				.filter(deletedId -> id == null || !deletedId.equals(id)).ifPresent(deletedId -> {
					throw new EntityAlreadyExistsException("Collection",
							request.name() + " for " + request.targetAudience() + " (deleted collection " + deletedId
									+ " still holds this name; restore it with POST /api/v1/admin/collections/"
									+ deletedId + "/restore, or pick another name)");
				});

		if (request.displayPosition() == DisplayPosition.HEADER) {
			collectionRepository
					.findByDisplayPositionAndTargetAudience(DisplayPosition.HEADER, request.targetAudience())
					.filter(existing -> id == null || !existing.getId().equals(id)).ifPresent(existing -> {
						throw new EntityAlreadyExistsException("Collection", "HEADER for " + request.targetAudience());
					});
		}
	}

	/**
	 * Conta as removidas de propósito: uk_collection_slug é uma constraint total,
	 * então uma coleção removida ainda ocupa o slug. Com o existsBySlug derivado —
	 * que o @SQLRestriction filtra
	 */
	private String generateUniqueSlug(String name) {
		String baseSlug = generateSlug(name);
		String uniqueSlug = baseSlug;
		int counter = 1;
		while (collectionRepository.existsBySlugIncludingDeleted(uniqueSlug)) {
			uniqueSlug = baseSlug + "-" + counter;
			counter++;
		}
		return uniqueSlug;
	}
}
