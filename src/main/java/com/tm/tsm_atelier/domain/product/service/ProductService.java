package com.tm.tsm_atelier.domain.product.service;

import static com.tm.tsm_atelier.common.utils.SlugUtils.generateSlug;

import com.tm.tsm_atelier.common.dto.CustomPageImpl;
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

		// #1 — Gerar slug definitivo único antes do save
		product.setSlug(generateUniqueSlug(product.getName()));

		Product savedProduct = productRepository.save(product);

		return productMapper.toAdminResponse(savedProduct);
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

		if (product.getSlug() == null) {
			product.setSlug(generateUniqueSlug(request.name()));
		}

		mergeColors(product, request.colors());

		Product updatedProduct = productRepository.save(product);
		return productMapper.toAdminResponse(updatedProduct);
	}

	private void mergeColors(Product product, List<ProductColorRequestDTO> requestColors) {
		java.util.Set<ProductColor> existingColors = product.getColors();
		java.util.Map<Long, ProductColor> existingById = existingColors.stream().filter(c -> c.getId() != null).collect(
				java.util.stream.Collectors.toMap(ProductColor::getId, java.util.function.Function.identity()));

		java.util.Set<ProductColor> keptColors = new java.util.LinkedHashSet<>();

		if (requestColors != null) {
			for (var colorReq : requestColors) {
				ProductColor colorEntity;
				if (colorReq.id() != null) {
					colorEntity = existingById.get(colorReq.id());
					if (colorEntity == null) {
						throw new ResourceNotFoundException("ProductColor", colorReq.id());
					}
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
				keptColors.add(colorEntity);
			}
		}

		existingColors.removeIf(existing -> !keptColors.contains(existing));
		keptColors.stream().filter(u -> u.getId() == null).forEach(existingColors::add);
	}

	private void mergeSkus(ProductColor colorEntity, List<ProductSKURequestDTO> requestSkus) {
		java.util.Set<ProductSKU> existingSkus = colorEntity.getSkus();
		java.util.Map<Long, ProductSKU> existingById = existingSkus.stream().filter(s -> s.getId() != null)
				.collect(java.util.stream.Collectors.toMap(ProductSKU::getId, java.util.function.Function.identity()));

		java.util.Set<ProductSKU> keptSkus = new java.util.LinkedHashSet<>();

		if (requestSkus != null) {
			for (var skuReq : requestSkus) {
				ProductSKU skuEntity;
				if (skuReq.id() != null) {
					skuEntity = existingById.get(skuReq.id());
					if (skuEntity == null) {
						throw new ResourceNotFoundException("ProductSKU", skuReq.id());
					}

					if (!skuEntity.getSkuCode().equals(skuReq.skuCode())
							&& skuRepository.existsBySkuCodeAndIdNot(skuReq.skuCode(), skuReq.id())) {
						throw new EntityAlreadyExistsException("SKU", skuReq.skuCode());
					}

					skuEntity.setSize(skuReq.size());
					skuEntity.setSkuCode(skuReq.skuCode());
					skuEntity.setStockQuantity(skuReq.stockQuantity());
				} else {
					if (skuRepository.existsBySkuCodeIncludingDeleted(skuReq.skuCode())) {
						throw new EntityAlreadyExistsException("SKU", skuReq.skuCode());
					}
					skuEntity = ProductSKU.builder().productColor(colorEntity).size(skuReq.size())
							.skuCode(skuReq.skuCode()).stockQuantity(skuReq.stockQuantity()).build();
				}
				keptSkus.add(skuEntity);
			}
		}

		existingSkus.removeIf(existing -> !keptSkus.contains(existing));
		keptSkus.stream().filter(u -> u.getId() == null).forEach(existingSkus::add);
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

		Page<ProductSummaryDTO> mappedPage = page.map(productMapper::toSummary);
		return new CustomPageImpl<>(mappedPage.getContent(), mappedPage.getPageable(), mappedPage.getTotalElements());
	}

	@Transactional(readOnly = true)
	public Page<ProductSummaryDTO> searchAdmin(String searchTerm, Category category, TargetAudience targetAudience,
			Long collectionId, BigDecimal minPrice, BigDecimal maxPrice, Boolean isFeatured, Pageable pageable) {

		Specification<Product> spec = Specification.where(ProductSpecification.search(searchTerm))
				.and(ProductSpecification.hasCategory(category))
				.and(ProductSpecification.hasTargetAudience(targetAudience))
				.and(ProductSpecification.hasCollectionId(collectionId))
				.and(ProductSpecification.priceBetween(minPrice, maxPrice))
				.and(ProductSpecification.isFeatured(isFeatured));

		Page<Product> page = productRepository.findAll(spec, pageable);

		if (page.hasContent()) {
			productRepository.fetchColorsForProducts(page.getContent());
		}

		Page<ProductSummaryDTO> mappedPage = page.map(productMapper::toSummary);
		return new CustomPageImpl<>(mappedPage.getContent(), mappedPage.getPageable(), mappedPage.getTotalElements());
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
		return productMapper.toCatalogResponse(product);
	}

	@Transactional(readOnly = true)
	public ProductResponseDTO findAdminById(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
		return productMapper.toAdminResponse(product);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = "catalog_slug", key = "#slug")
	public ProductResponseDTO findBySlug(String slug) {
		Product product = productRepository.findBySlugAndDeletedAtIsNull(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Product", slug));
		return productMapper.toCatalogResponse(product);
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

	private String generateUniqueSlug(String name) {
		String baseSlug = generateSlug(name);
		String uniqueSlug = baseSlug;
		int counter = 1;
		while (productRepository.existsBySlug(uniqueSlug)) {
			uniqueSlug = baseSlug + "-" + counter;
			counter++;
		}
		return uniqueSlug;
	}
}
