package com.tm.tsm_atelier.domain.product.service;

import static com.tm.tsm_atelier.common.utils.SlugUtils.generateSlug;

import com.tm.tsm_atelier.common.exception.custom.EntityAlreadyExistsException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.domain.collection.repository.CollectionRepository;
import com.tm.tsm_atelier.domain.product.dto.*;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.entity.ProductColor;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import com.tm.tsm_atelier.domain.product.mapper.ProductMapper;
import com.tm.tsm_atelier.domain.product.repository.ProductColorRepository;
import com.tm.tsm_atelier.domain.product.repository.ProductRepository;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import com.tm.tsm_atelier.domain.product.repository.ProductSpecification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;
	private final ProductColorRepository colorRepository;
	private final ProductSKURepository skuRepository;
	private final CollectionRepository collectionRepository;
	private final ProductMapper productMapper;

	@Transactional
	@CacheEvict(value = {"catalog_products", "catalog_slug"}, allEntries = true)
	public ProductResponseDTO create(ProductRequestDTO request) {
		if (productRepository.existsByNameAndDeletedAtIsNull(request.name()))
			throw new EntityAlreadyExistsException("Product", request.name());

		// #8 — Validação de Category vs TargetAudience
		validateCategoryForAudience(request.category(), request.targetAudience());

		validateFabricComposition(request.fabricCompositions());

		Product product = productMapper.toEntity(request);

		// #7 — Preservar ordem das instruções de cuidado
		if (request.careInstructions() != null) {
			product.setCareInstructions(new LinkedHashSet<>(request.careInstructions()));
		}

		if (request.collectionId() != null) {
			product.setCollection(collectionRepository.findById(request.collectionId())
					.orElseThrow(() -> new ResourceNotFoundException("Collection", request.collectionId())));
		}

		// #2 — Validação de SKU em batch (1 query ao invés de N)
		validateSkuCodesUniqueness(request.colors());

		request.colors().forEach(colorDto -> {
			ProductColor color = buildColor(colorDto, product);

			colorDto.skus().forEach(skuDto -> {
				ProductSKU sku = buildSku(skuDto, color);
				color.getSkus().add(sku);
			});
			product.getColors().add(color);
		});

		Product savedProduct = productRepository.save(product);

		// #1 — Gerar slug definitivo com ID após o save
		savedProduct.setSlug(generateSlug(savedProduct.getName()) + "-" + savedProduct.getId());

		return productMapper.toResponse(savedProduct);
	}

	@Transactional
	@CacheEvict(value = {"catalog_products", "catalog_slug"}, allEntries = true)
	public ProductResponseDTO update(Long id, ProductRequestDTO request) {
		Product product = productRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));

		if (!product.getName().equals(request.name())
				&& productRepository.existsByNameAndDeletedAtIsNull(request.name())) {
			throw new EntityAlreadyExistsException("Product", request.name());
		}

		// #8 — Validação de Category vs TargetAudience
		validateCategoryForAudience(request.category(), request.targetAudience());

		validateFabricComposition(request.fabricCompositions());

		productMapper.updateEntityFromRequest(request, product);

		// #7 — Preservar ordem das instruções de cuidado
		if (request.careInstructions() != null) {
			product.getCareInstructions().clear();
			product.getCareInstructions().addAll(request.careInstructions());
		}

		if (request.collectionId() != null) {
			product.setCollection(collectionRepository.findById(request.collectionId())
					.orElseThrow(() -> new ResourceNotFoundException("Collection", request.collectionId())));
		} else {
			product.setCollection(null);
		}

		product.setSlug(generateSlug(request.name()) + "-" + product.getId());

		mergeColors(product, request.colors());

		Product updatedProduct = productRepository.save(product);
		return productMapper.toResponse(updatedProduct);
	}

	private void mergeColors(Product product, List<ProductColorRequestDTO> requestColors) {
		java.util.Set<ProductColor> existingColors = product.getColors();
		List<ProductColor> updatedColors = new java.util.ArrayList<>();

		if (requestColors != null) {
			for (var colorReq : requestColors) {
				ProductColor colorEntity;
				if (colorReq.id() != null) {
					colorEntity = existingColors.stream().filter(c -> colorReq.id().equals(c.getId())).findFirst()
							.orElseThrow(() -> new ResourceNotFoundException("ProductColor", colorReq.id()));
					colorEntity.setColorName(colorReq.colorName());
					colorEntity.setColorHex(colorReq.colorHex());
					colorEntity.setCoverImageUrl(colorReq.coverImageUrl());
					colorEntity.setHoverImageUrl(colorReq.hoverImageUrl());

					colorEntity.getGalleryImages().clear();
					if (colorReq.galleryImages() != null) {
						colorEntity.getGalleryImages().addAll(colorReq.galleryImages());
					}
				} else {
					colorEntity = ProductColor.builder().product(product).colorName(colorReq.colorName())
							.colorHex(colorReq.colorHex()).coverImageUrl(colorReq.coverImageUrl())
							.hoverImageUrl(colorReq.hoverImageUrl())
							.galleryImages(colorReq.galleryImages() == null
									? new java.util.LinkedHashSet<>()
									: new java.util.LinkedHashSet<>(colorReq.galleryImages()))
							.build();
				}

				mergeSkus(colorEntity, colorReq.skus());
				updatedColors.add(colorEntity);
			}
		}

		existingColors.removeIf(existing -> updatedColors.stream().noneMatch(u -> u == existing));
		updatedColors.stream().filter(u -> u.getId() == null).forEach(existingColors::add);
	}

	private void mergeSkus(ProductColor colorEntity, List<ProductSKURequestDTO> requestSkus) {
		java.util.Set<ProductSKU> existingSkus = colorEntity.getSkus();
		List<ProductSKU> updatedSkus = new java.util.ArrayList<>();

		if (requestSkus != null) {
			// #4 — Validação correta de unicidade: exclui o próprio SKU da checagem
			for (var skuReq : requestSkus) {
				if (skuReq.id() != null) {
					// SKU existente com código alterado — verificar se o novo código já existe em
					// outro registro
					ProductSKU existingSku = existingSkus.stream().filter(s -> skuReq.id().equals(s.getId()))
							.findFirst().orElseThrow(() -> new ResourceNotFoundException("ProductSKU", skuReq.id()));

					if (!existingSku.getSkuCode().equals(skuReq.skuCode())
							&& skuRepository.existsBySkuCodeAndIdNot(skuReq.skuCode(), skuReq.id())) {
						throw new EntityAlreadyExistsException("SKU", skuReq.skuCode());
					}

					existingSku.setSize(skuReq.size());
					existingSku.setSkuCode(skuReq.skuCode());
					existingSku.setStockQuantity(skuReq.stockQuantity());
					updatedSkus.add(existingSku);
				} else {
					// SKU novo — verificar se o código já existe no banco
					if (skuRepository.findBySkuCode(skuReq.skuCode()).isPresent()) {
						throw new EntityAlreadyExistsException("SKU", skuReq.skuCode());
					}
					ProductSKU skuEntity = ProductSKU.builder().productColor(colorEntity).size(skuReq.size())
							.skuCode(skuReq.skuCode()).stockQuantity(skuReq.stockQuantity()).build();
					updatedSkus.add(skuEntity);
				}
			}
		}

		existingSkus.removeIf(existing -> updatedSkus.stream().noneMatch(u -> u == existing));
		updatedSkus.stream().filter(u -> u.getId() == null).forEach(existingSkus::add);
	}

	@Transactional
	@CacheEvict(value = {"catalog_products", "catalog_slug"}, allEntries = true)
	public void delete(Long id) {
		Product product = productRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));

		LocalDateTime now = LocalDateTime.now();

		// #5 — Soft-delete propagado para cores e SKUs
		product.getColors().forEach(color -> {
			color.getSkus().forEach(sku -> {
				sku.setDeletedAt(now);
			});
			color.setDeletedAt(now);
		});

		product.setDeletedAt(now);
		product.setActive(false);
		productRepository.save(product);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "catalog_products", key = "{#searchTerm, #category, #targetAudience, #collectionId, #minPrice, #maxPrice, #isFeatured, #pageable.pageNumber, #pageable.pageSize, #pageable.sort.toString()}")
	public Page<ProductSummaryDTO> searchCatalog(String searchTerm, Category category, TargetAudience targetAudience,
			Long collectionId, BigDecimal minPrice, BigDecimal maxPrice, Boolean isFeatured, Pageable pageable) {

		Specification<Product> spec = Specification.where(ProductSpecification.isNotDeleted())
				.and(ProductSpecification.isActive()).and(ProductSpecification.search(searchTerm))
				.and(ProductSpecification.hasCategory(category))
				.and(ProductSpecification.hasTargetAudience(targetAudience))
				.and(ProductSpecification.hasCollectionId(collectionId))
				.and(ProductSpecification.priceBetween(minPrice, maxPrice))
				.and(ProductSpecification.isFeatured(isFeatured));

		Page<Product> page = productRepository.findAll(spec, pageable);

		if (page.hasContent()) {
			// #6 — O retorno é descartado intencionalmente: o Hibernate L1 cache hidrata
			// os proxies de colors nas mesmas instâncias gerenciadas dentro desta
			// transação.
			productRepository.fetchColorsForProducts(page.getContent());
		}

		org.springframework.data.domain.Page<ProductSummaryDTO> mappedPage = page.map(productMapper::toSummary);
		return new com.tm.tsm_atelier.common.dto.CustomPageImpl<>(mappedPage.getContent(), mappedPage.getPageable(),
				mappedPage.getTotalElements());
	}

	private ProductColor buildColor(ProductColorRequestDTO dto, Product product) {
		return ProductColor.builder().product(product).colorName(dto.colorName()).colorHex(dto.colorHex())
				.coverImageUrl(dto.coverImageUrl()).hoverImageUrl(dto.hoverImageUrl())
				.galleryImages(
						dto.galleryImages() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(dto.galleryImages()))
				.build();
	}

	private ProductSKU buildSku(ProductSKURequestDTO dto, ProductColor color) {
		return ProductSKU.builder().productColor(color).size(dto.size()).skuCode(dto.skuCode())
				.stockQuantity(dto.stockQuantity()).build();
	}

	@Transactional(readOnly = true)
	public ProductResponseDTO findById(Long id) {
		Product product = productRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
		return productMapper.toResponse(product);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "catalog_slug", key = "#slug")
	public ProductResponseDTO findBySlug(String slug) {
		Product product = productRepository.findBySlugAndDeletedAtIsNull(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Product", slug));
		return productMapper.toResponse(product);
	}

	// ============================================================
	// ❌ VERSÃO ANTIGA — PROBLEMA N+1 (remover depois de testar)
	// ============================================================
	@Transactional(readOnly = true)
	public List<ProductResponseDTO> findAllWithNPlusOne() {
		log.info("========== [N+1] Iniciando findAll SEM @EntityGraph ==========");
		long start = System.currentTimeMillis();

		List<ProductResponseDTO> result = productRepository.findAll().stream().map(productMapper::toResponse).toList();

		long duration = System.currentTimeMillis() - start;
		log.info("========== [N+1] findAll finalizado em {} ms — {} produtos ==========", duration, result.size());
		return result;
	}

	// --- Métodos auxiliares de validação ---

	private void validateCategoryForAudience(Category category, TargetAudience audience) {
		if (!category.isValidFor(audience)) {
			throw new IllegalArgumentException("Category " + category + " is not valid for audience " + audience);
		}
	}

	private void validateSkuCodesUniqueness(List<ProductColorRequestDTO> colors) {
		List<String> allSkuCodes = colors.stream().flatMap(c -> c.skus().stream()).map(ProductSKURequestDTO::skuCode)
				.toList();

		if (!allSkuCodes.isEmpty()) {
			List<String> existing = skuRepository.findExistingSkuCodes(allSkuCodes);
			if (!existing.isEmpty()) {
				throw new EntityAlreadyExistsException("SKU", String.join(", ", existing));
			}
		}
	}

	private void validateFabricComposition(List<FabricCompositionRequestDTO> compositions) {
		if (compositions == null || compositions.isEmpty()) {
			return;
		}
		int totalPercentage = compositions.stream().mapToInt(FabricCompositionRequestDTO::percentage).sum();
		if (totalPercentage != 100) {
			throw new IllegalArgumentException(
					"Total fabric composition percentage must be exactly 100%, but was " + totalPercentage + "%");
		}
	}
}
