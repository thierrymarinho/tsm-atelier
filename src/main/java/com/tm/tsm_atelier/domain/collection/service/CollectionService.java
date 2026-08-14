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

	/**
	 * Cache próprio, e não o {@code catalog_slug} do produto. Um cache do Redis tem
	 * um serializer de valor por nome: enquanto os dois dividiram o mesmo cache, a
	 * coleção era gravada e <strong>nunca lida de volta</strong> — o JSON dela era
	 * forçado para dentro de {@code ProductResponseDTO} e quebrava. Um prefixo na
	 * chave resolvia a colisão de nome, que não era o problema.
	 *
	 * <p>
	 * A recíproca não vale, e por isso a invalidação é assimétrica: mexer numa
	 * coleção invalida os dois caches, porque {@code ProductResponseDTO} carrega a
	 * coleção dentro de si e um rename mudaria o payload do produto; mexer num
	 * produto não toca este aqui, porque {@code CollectionResponseDTO} não sabe
	 * nada sobre produtos.
	 *
	 * <p>
	 * Derivada, e não nativa: o {@code @SQLRestriction} da entidade já esconde as
	 * removidas, que é exatamente o que o catálogo público precisa.
	 */
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
	 * Excluir a coleção <strong>desassocia</strong> os produtos por padrão; passar
	 * {@code cascadeProducts} exclui cada um deles junto.
	 *
	 * <p>
	 * Antes o comportamento em cascata era o único, e não estava dito em lugar
	 * nenhum: {@code DELETE /api/v1/admin/collections/{id}} apagava todo o catálogo
	 * daquela coleção. Um admin reorganizando vitrine apagava a loja sem nenhum
	 * aviso.
	 *
	 * <p>
	 * O filtro por {@code getDeletedAt() == null} corrige a outra metade do
	 * problema: {@code Product} não tem {@code @SQLRestriction}, então produtos já
	 * removidos continuavam vindo na lista, {@code productService.delete} não os
	 * encontrava e a exclusão inteira voltava como 404 "Product not found" — uma
	 * coleção que já tivesse perdido um produto ficava impossível de excluir.
	 */
	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_COLLECTIONS, CacheNames.CATALOG_SLUG_COLLECTION, CacheNames.CATALOG_SLUG,
			CacheNames.CATALOG_PRODUCTS}, allEntries = true)
	public void delete(Long id, boolean cascadeProducts) {
		Collection collection = collectionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Collection", id));

		// Cópia antes de iterar: os dois ramos abaixo mexem no vínculo entre produto
		// e coleção, que é a origem desta própria lista.
		List<Product> livingProducts = collection.getProducts().stream().filter(p -> p.getDeletedAt() == null).toList();

		if (cascadeProducts) {
			livingProducts.forEach(product -> productService.delete(product.getId()));
		} else {
			livingProducts.forEach(product -> product.setCollection(null));
		}

		// O modo entra no registro porque as duas exclusões são irreversíveis de
		// formas diferentes: desassociar perde o vínculo, e a restauração não o
		// reconstrói; a cascata deixa cada produto com a sua própria rota de volta.
		auditService.record(AuditedEntity.COLLECTION, id, AuditAction.DELETED,
				livingProducts.size() + " products " + (cascadeProducts ? "deleted" : "detached"));

		collection.setDeletedAt(LocalDateTime.now());
		collection.setActive(false);
		collectionRepository.delete(collection);
	}

	/**
	 * Desfaz um {@link #delete(Long, boolean)}. Sem isto a exclusão de coleção era
	 * um caminho só de ida — e pior do que a de produto, porque o
	 * {@code @SQLRestriction} de {@code Collection} some com o registro de toda
	 * consulta: o admin nem via o item na lista para suspeitar que ele existia.
	 *
	 * <p>
	 * Nome e slug nunca entram em conflito aqui, e vale registrar o porquê: as duas
	 * constraints são totais, então a coleção removida <strong>nunca
	 * soltou</strong> nenhum dos dois — ninguém pôde ocupá-los no intervalo.
	 *
	 * <p>
	 * A posição de destaque é o contrário. Os três índices parciais do V2 excluem
	 * as removidas, justamente para que uma coleção no lixo não bloqueie o
	 * HOME_MAIN de todo o site; a contrapartida é que a posição fica livre, e
	 * alguém pode tê-la ocupado. Por isso a coleção volta com {@code NONE}, e não
	 * com a posição que tinha.
	 *
	 * <p>
	 * <strong>Os produtos não voltam junto, e não é omissão.</strong> Na exclusão
	 * padrão eles foram desassociados — o vínculo deixou de existir, e não há o que
	 * restaurar. Na exclusão em cascata eles foram removidos e continuam removidos:
	 * cada um tem sua própria rota, com suas próprias checagens de código de SKU, e
	 * encadear isso aqui produziria uma restauração que falha pela metade.
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

		// Releitura obrigatória: o UPDATE nativo limpa o contexto de persistência.
		Collection restored = collectionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Collection", id));

		// Volta inativa, como o produto: recuperar o cadastro e devolvê-lo à vitrine
		// são duas decisões, e juntá-las faria um clique republicar uma coleção sem
		// que ninguém conferisse que ela está vazia.
		restored.setActive(false);

		// A posição de destaque também não volta, pelo mesmo raciocínio levado até o
		// fim — mas quem a zera é o próprio UPDATE de restoreCollection, e não uma
		// linha aqui: limpar o deleted_at sozinho já devolveria a linha ao índice
		// carregando a posição antiga, e o UPDATE morreria antes de chegar neste
		// ponto. Ver o javadoc do método no repositório.

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

		// A checagem acima passa por cima das removidas — ela é JPQL, e o
		// @SQLRestriction as esconde. Só que uk_collection_name_audience é total: o
		// nome continua ocupado, e sem esta segunda checagem o insert quebrava no
		// banco e voltava como 409 "A data conflict occurred", apontando um registro
		// que não aparece em lugar nenhum da interface.
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
	 * Conta as removidas de propósito: {@code uk_collection_slug} é uma constraint
	 * total, então uma coleção removida ainda ocupa o slug. Com o
	 * {@code existsBySlug} derivado — que o {@code @SQLRestriction} filtra — este
	 * laço devolvia um slug já ocupado e o insert quebrava no banco.
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
