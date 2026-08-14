package com.tm.tsm_atelier.domain.product.service;

import static com.tm.tsm_atelier.common.utils.SlugUtils.generateSlug;

import com.tm.tsm_atelier.common.dto.CustomPageImpl;
import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.common.exception.custom.EntityAlreadyExistsException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.config.CacheNames;
import com.tm.tsm_atelier.domain.admin.entity.AuditAction;
import com.tm.tsm_atelier.domain.admin.entity.AuditedEntity;
import com.tm.tsm_atelier.domain.admin.service.AuditService;
import com.tm.tsm_atelier.domain.collection.repository.CollectionRepository;
import com.tm.tsm_atelier.domain.product.dto.*;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.entity.ProductColor;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.enums.CareAxis;
import com.tm.tsm_atelier.domain.product.enums.CareInstruction;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.Material;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import com.tm.tsm_atelier.domain.product.mapper.ProductMapper;
import com.tm.tsm_atelier.domain.product.repository.ProductColorRepository;
import com.tm.tsm_atelier.domain.product.repository.ProductRepository;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import com.tm.tsm_atelier.domain.product.repository.ProductSpecification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;
	private final ProductSKURepository skuRepository;
	private final ProductColorRepository colorRepository;
	private final CollectionRepository collectionRepository;
	private final ProductMapper productMapper;
	private final AuditService auditService;

	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_PRODUCTS, CacheNames.CATALOG_SLUG}, allEntries = true)
	public AdminProductResponseDTO create(ProductRequestDTO request) {
		if (productRepository.existsByNameAndDeletedAtIsNull(request.name()))
			throw new EntityAlreadyExistsException("Product", request.name());

		validateCategoryForAudience(request.category(), request.targetAudience());

		validateFabricComposition(request.fabricCompositions());

		validateCareInstructions(request.careInstructions());

		validatePromotionalPrice(request);

		Product product = productMapper.toEntity(request);

		if (request.careInstructions() != null) {
			product.setCareInstructions(new LinkedHashSet<>(request.careInstructions()));
		}

		if (request.collectionId() != null) {
			product.setCollection(collectionRepository.findById(request.collectionId())
					.orElseThrow(() -> new ResourceNotFoundException("Collection", request.collectionId())));
		}

		request.colors().forEach(colorDto -> {
			ProductColor color = buildColor(colorDto, product);

			colorDto.skus().forEach(skuDto -> {
				ProductSKU sku = buildSku(skuDto, color);
				color.getSkus().add(sku);
			});
			product.getColors().add(color);
		});

		product.setSlug(generateUniqueSlug(product.getName()));

		Product savedProduct = productRepository.save(product);

		auditService.record(AuditedEntity.PRODUCT, savedProduct.getId(), AuditAction.CREATED);

		return productMapper.toAdminResponse(savedProduct);
	}

	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_PRODUCTS, CacheNames.CATALOG_SLUG}, allEntries = true)
	public AdminProductResponseDTO update(Long id, ProductRequestDTO request) {
		Product product = productRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));

		if (!product.getName().equals(request.name())
				&& productRepository.existsByNameAndDeletedAtIsNull(request.name())) {
			throw new EntityAlreadyExistsException("Product", request.name());
		}

		validateCategoryForAudience(request.category(), request.targetAudience());

		validateFabricComposition(request.fabricCompositions());

		validateCareInstructions(request.careInstructions());

		validatePromotionalPrice(request);

		BigDecimal previousPromotionalPrice = product.getPromotionalPrice();

		productMapper.updateEntityFromRequest(request, product);

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

		auditService.record(AuditedEntity.PRODUCT, id, AuditAction.UPDATED);
		auditService.recordChange(AuditedEntity.PRODUCT, id, AuditAction.PROMOTIONAL_PRICE_CHANGED,
				previousPromotionalPrice, updatedProduct.getPromotionalPrice());

		return productMapper.toAdminResponse(updatedProduct);
	}

	private void mergeColors(Product product, List<ProductColorRequestDTO> requestColors) {
		Set<ProductColor> existingColors = product.getColors();
		Map<Long, ProductColor> existingById = existingColors.stream().filter(c -> c.getId() != null)
				.collect(Collectors.toMap(ProductColor::getId, Function.identity()));

		Set<ProductColor> keptColors = new LinkedHashSet<>();

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
									? new LinkedHashSet<>()
									: new LinkedHashSet<>(colorReq.galleryImages()))
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
		Set<ProductSKU> existingSkus = colorEntity.getSkus();
		Map<Long, ProductSKU> existingById = existingSkus.stream().filter(s -> s.getId() != null)
				.collect(Collectors.toMap(ProductSKU::getId, Function.identity()));

		Set<ProductSKU> keptSkus = new LinkedHashSet<>();

		if (requestSkus != null) {
			for (var skuReq : requestSkus) {
				ProductSKU skuEntity;
				if (skuReq.id() != null) {
					skuEntity = existingById.get(skuReq.id());
					if (skuEntity == null) {
						throw new ResourceNotFoundException("ProductSKU", skuReq.id());
					}

					rejectStockOnExistingSku(skuEntity, skuReq);

					// O código não é tocado: ele foi congelado dentro de todo pedido que
					// já levou este SKU, e reescrevê-lo aqui faria o catálogo e o
					// histórico discordarem sobre a mesma peça.
					skuEntity.setSize(skuReq.size());
				} else {
					skuEntity = ProductSKU.builder().productColor(colorEntity).size(skuReq.size())
							.skuCode(generateSkuCode()).stockQuantity(requireInitialStock(skuReq)).build();
				}
				keptSkus.add(skuEntity);
			}
		}

		existingSkus.removeIf(existing -> !keptSkus.contains(existing));
		keptSkus.stream().filter(u -> u.getId() == null).forEach(existingSkus::add);
	}

	/**
	 * O formulário de produto edita cadastro; estoque muda a cada venda e tem porta
	 * própria. Enquanto os dois compartilhavam esta gravação, salvar uma correção
	 * de descrição feita sobre uma tela aberta há minutos devolvia ao estoque
	 * unidades já vendidas — e a defesa contra isso obrigava todo salvamento a
	 * carregar a versão de cada SKU, transformando qualquer venda concorrente em
	 * 409 numa edição que nem tocava no número.
	 *
	 * Recusar, e não ignorar: aceitar o campo em silêncio devolveria 200 para uma
	 * mudança que não aconteceu, que é a pior das três respostas possíveis.
	 */
	private void rejectStockOnExistingSku(ProductSKU skuEntity, ProductSKURequestDTO skuReq) {
		if (skuReq.stockQuantity() != null) {
			throw new BusinessRuleException("Stock for SKU " + skuEntity.getSkuCode()
					+ " cannot be changed through the product form. Omit stockQuantity for existing SKUs and use PATCH /api/v1/admin/skus/"
					+ skuEntity.getId() + "/stock instead.");
		}
	}

	private Integer requireInitialStock(ProductSKURequestDTO skuReq) {
		if (skuReq.stockQuantity() == null) {
			throw new BusinessRuleException("New SKU (size " + skuReq.size() + ") requires an initial stockQuantity.");
		}

		return skuReq.stockQuantity();
	}

	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_PRODUCTS, CacheNames.CATALOG_SLUG}, allEntries = true)
	public void delete(Long id) {
		Product product = productRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));

		LocalDateTime now = LocalDateTime.now();

		product.getColors().forEach(color -> {
			color.getSkus().forEach(sku -> {
				sku.setDeletedAt(now);
			});
			color.setDeletedAt(now);
		});

		product.setDeletedAt(now);
		product.setActive(false);
		productRepository.save(product);

		// deleted_at diz quando, e nunca quem.
		auditService.record(AuditedEntity.PRODUCT, id, AuditAction.DELETED);
	}

	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_PRODUCTS, CacheNames.CATALOG_SLUG}, allEntries = true)
	public AdminProductResponseDTO restore(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));

		if (product.getDeletedAt() == null) {
			throw new BusinessRuleException("Product " + id + " is not deleted.");
		}

		LocalDateTime deletedAt = product.getDeletedAt();

		List<String> blockedCodes = skuRepository.findSkuCodesBlockingRestore(id, deletedAt);
		if (!blockedCodes.isEmpty()) {
			throw new EntityAlreadyExistsException("SKU", String.join(", ", blockedCodes));
		}

		skuRepository.restoreSkusOfProduct(id, deletedAt);
		colorRepository.restoreColorsOfProduct(id, deletedAt);

		Product restored = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
		restored.setDeletedAt(null);

		auditService.record(AuditedEntity.PRODUCT, id, AuditAction.RESTORED);

		return productMapper.toAdminResponse(restored);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = CacheNames.CATALOG_PRODUCTS)
	public Page<ProductSummaryDTO> searchCatalog(ProductSearchFilter filter, Pageable pageable) {

		Specification<Product> spec = Specification.where(ProductSpecification.isNotDeleted())
				.and(ProductSpecification.isActive()).and(matches(filter));

		return toSummaryPage(productRepository.findAll(spec, pageable), productMapper::toSummary);
	}

	@Transactional(readOnly = true)
	public Page<AdminProductSummaryDTO> searchAdmin(ProductSearchFilter filter, Pageable pageable) {
		return toSummaryPage(productRepository.findAll(Specification.where(matches(filter)), pageable),
				productMapper::toAdminSummary);
	}

	private Specification<Product> matches(ProductSearchFilter filter) {
		return Specification.where(ProductSpecification.search(filter.searchTerm()))
				.and(ProductSpecification.hasCategory(filter.category()))
				.and(ProductSpecification.hasTargetAudience(filter.targetAudience()))
				.and(ProductSpecification.hasCollectionId(filter.collectionId()))
				.and(ProductSpecification.priceBetween(filter.minPrice(), filter.maxPrice()))
				.and(ProductSpecification.isFeatured(filter.isFeatured()))
				.and(ProductSpecification.isOnSale(filter.onSale()));
	}

	private <T> Page<T> toSummaryPage(Page<Product> page, Function<Product, T> toCard) {
		if (page.hasContent()) {
			productRepository.fetchColorsForProducts(page.getContent());
		}

		Page<T> mappedPage = page.map(toCard);
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
		return ProductSKU.builder().productColor(color).size(dto.size()).skuCode(generateSkuCode())
				.stockQuantity(requireInitialStock(dto)).build();
	}

	private String generateSkuCode() {
		return "TSM-%06d".formatted(skuRepository.nextSkuCodeNumber());
	}

	@Transactional(readOnly = true)
	public ProductResponseDTO findById(Long id) {
		Product product = productRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
		return productMapper.toCatalogResponse(product);
	}

	@Transactional(readOnly = true)
	public AdminProductResponseDTO findAdminById(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
		return productMapper.toAdminResponse(product);
	}

	@Transactional(readOnly = true)
	@Cacheable(value = CacheNames.CATALOG_SLUG, key = "#slug")
	public ProductResponseDTO findBySlug(String slug) {
		Product product = productRepository.findBySlugAndDeletedAtIsNull(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Product", slug));
		return productMapper.toCatalogResponse(product);
	}

	private void validatePromotionalPrice(ProductRequestDTO request) {
		if (request.promotionalPrice() == null) {
			return;
		}

		if (request.promotionalPrice().compareTo(request.price()) >= 0) {
			throw new BusinessRuleException("The promotional price must be lower than the regular price.");
		}
	}

	private void validateCategoryForAudience(Category category, TargetAudience audience) {
		if (!category.isValidFor(audience)) {
			throw new BusinessRuleException("Category " + category + " is not valid for audience " + audience);
		}
	}

	private void validateFabricComposition(List<FabricCompositionRequestDTO> compositions) {
		if (compositions == null || compositions.isEmpty()) {
			return;
		}

		validateNoDuplicateMaterials(compositions);

		int totalPercentage = compositions.stream().mapToInt(FabricCompositionRequestDTO::percentage).sum();
		if (totalPercentage != 100) {
			throw new BusinessRuleException(
					"Total fabric composition percentage must be exactly 100%, but was " + totalPercentage + "%");
		}
	}

	private void validateNoDuplicateMaterials(List<FabricCompositionRequestDTO> compositions) {
		Set<Material> seen = EnumSet.noneOf(Material.class);
		List<String> duplicates = compositions.stream().map(FabricCompositionRequestDTO::material)
				.filter(material -> !seen.add(material)).distinct().map(Material::getLabel).toList();

		if (!duplicates.isEmpty()) {
			throw new BusinessRuleException(
					"Fabric composition cannot repeat the same material: " + String.join(", ", duplicates));
		}
	}

	private void validateCareInstructions(List<CareInstruction> careInstructions) {
		if (careInstructions == null || careInstructions.isEmpty()) {
			return;
		}

		Map<CareAxis, CareInstruction> chosen = new EnumMap<>(CareAxis.class);
		List<String> conflicts = new ArrayList<>();

		for (CareInstruction instruction : careInstructions) {
			CareInstruction previous = chosen.putIfAbsent(instruction.getAxis(), instruction);
			if (previous != null && previous != instruction) {
				conflicts.add(instruction.getAxis().getLabel() + " (" + previous.getLabel() + " / "
						+ instruction.getLabel() + ")");
			}
		}

		if (!conflicts.isEmpty()) {
			throw new BusinessRuleException(
					"Care instructions must not give two answers for the same axis: " + String.join(", ", conflicts));
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
