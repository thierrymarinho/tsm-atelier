package com.tm.tsm_atelier.domain.product.service;

import static com.tm.tsm_atelier.common.utils.SlugUtils.generateSlug;

import com.tm.tsm_atelier.common.dto.CustomPageImpl;
import com.tm.tsm_atelier.common.exception.custom.EntityAlreadyExistsException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.config.CacheNames;
import com.tm.tsm_atelier.domain.collection.repository.CollectionRepository;
import com.tm.tsm_atelier.domain.product.dto.*;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.entity.ProductColor;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import com.tm.tsm_atelier.domain.product.mapper.ProductMapper;
import com.tm.tsm_atelier.domain.product.repository.ProductRepository;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import com.tm.tsm_atelier.domain.product.repository.ProductSpecification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;
	private final ProductSKURepository skuRepository;
	private final CollectionRepository collectionRepository;
	private final ProductMapper productMapper;

	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_PRODUCTS, CacheNames.CATALOG_SLUG}, allEntries = true)
	public ProductResponseDTO create(ProductRequestDTO request) {
		if (productRepository.existsByNameAndDeletedAtIsNull(request.name()))
			throw new EntityAlreadyExistsException("Product", request.name());

		validateCategoryForAudience(request.category(), request.targetAudience());

		validateFabricComposition(request.fabricCompositions());

		validatePromotionalPrice(request);

		Product product = productMapper.toEntity(request);

		if (request.careInstructions() != null) {
			product.setCareInstructions(new LinkedHashSet<>(request.careInstructions()));
		}

		if (request.collectionId() != null) {
			product.setCollection(collectionRepository.findById(request.collectionId())
					.orElseThrow(() -> new ResourceNotFoundException("Collection", request.collectionId())));
		}

		validateSkuCodesUniqueness(request.colors());

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

		return productMapper.toAdminResponse(savedProduct);
	}

	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_PRODUCTS, CacheNames.CATALOG_SLUG}, allEntries = true)
	public ProductResponseDTO update(Long id, ProductRequestDTO request) {
		Product product = productRepository.findByIdAndDeletedAtIsNull(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));

		if (!product.getName().equals(request.name())
				&& productRepository.existsByNameAndDeletedAtIsNull(request.name())) {
			throw new EntityAlreadyExistsException("Product", request.name());
		}

		validateCategoryForAudience(request.category(), request.targetAudience());

		validateFabricComposition(request.fabricCompositions());

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

		logPromotionalPriceChange(id, previousPromotionalPrice, updatedProduct.getPromotionalPrice());

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
	}

	/**
	 * A chave do cache e deliberadamente omitida: sem ela o Spring usa o
	 * SimpleKeyGenerator, que monta a chave com todos os parametros do metodo. Como
	 * os filtros viajam num record, cujo toString() inclui todos os componentes, um
	 * filtro novo entra na chave sozinho.
	 *
	 * <p>
	 * Enquanto a chave era uma lista de parametros escrita a mao em SpEL, adicionar
	 * um filtro sem lembrar de adicionalo a chave fazia duas buscas diferentes
	 * compartilharem a mesma entrada no Redis — a vitrine de promocoes servia o
	 * catalogo inteiro, ou o contrario, dependendo de quem chegou primeiro com o
	 * cache frio. Compilava, passava nos testes e so aparecia em producao.
	 */
	@Transactional(readOnly = true)
	@Cacheable(value = CacheNames.CATALOG_PRODUCTS)
	public Page<ProductSummaryDTO> searchCatalog(ProductSearchFilter filter, Pageable pageable) {

		Specification<Product> spec = Specification.where(ProductSpecification.isNotDeleted())
				.and(ProductSpecification.isActive()).and(matches(filter));

		return toSummaryPage(productRepository.findAll(spec, pageable));
	}

	@Transactional(readOnly = true)
	public Page<ProductSummaryDTO> searchAdmin(ProductSearchFilter filter, Pageable pageable) {
		return toSummaryPage(productRepository.findAll(Specification.where(matches(filter)), pageable));
	}

	/**
	 * Os dois metodos de busca aplicam os mesmos filtros. O que os diferencia e so
	 * o que o admin enxerga a mais: produtos inativos e removidos.
	 */
	private Specification<Product> matches(ProductSearchFilter filter) {
		return Specification.where(ProductSpecification.search(filter.searchTerm()))
				.and(ProductSpecification.hasCategory(filter.category()))
				.and(ProductSpecification.hasTargetAudience(filter.targetAudience()))
				.and(ProductSpecification.hasCollectionId(filter.collectionId()))
				.and(ProductSpecification.priceBetween(filter.minPrice(), filter.maxPrice()))
				.and(ProductSpecification.isFeatured(filter.isFeatured()))
				.and(ProductSpecification.isOnSale(filter.onSale()));
	}

	private Page<ProductSummaryDTO> toSummaryPage(Page<Product> page) {
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
	@Cacheable(value = CacheNames.CATALOG_SLUG, key = "#slug")
	public ProductResponseDTO findBySlug(String slug) {
		Product product = productRepository.findBySlugAndDeletedAtIsNull(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Product", slug));
		return productMapper.toCatalogResponse(product);
	}

	/**
	 * A regra tambem existe como CHECK constraint na migration V3. Aqui ela vale
	 * pela mensagem: sem esta validacao o erro chegaria ao admin como violacao de
	 * integridade, sem dizer qual campo esta errado.
	 */
	private void validatePromotionalPrice(ProductRequestDTO request) {
		if (request.promotionalPrice() == null) {
			return;
		}

		if (request.promotionalPrice().compareTo(request.price()) >= 0) {
			throw new IllegalArgumentException("The promotional price must be lower than the regular price.");
		}
	}

	/**
	 * Mudanca de preco mexe em dinheiro e o banco guarda apenas o estado final —
	 * nao ha como saber depois quem colocou ou retirou uma promocao. Mesma razao do
	 * log de mudanca de status em OrderService.
	 */
	private void logPromotionalPriceChange(Long productId, BigDecimal previous, BigDecimal current) {
		if (java.util.Objects.equals(previous, current)) {
			return;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String actor = authentication == null ? "system" : authentication.getName();

		log.info("Product {} promotional price changed from {} to {} by {}", productId, previous, current, actor);
	}

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

		validateNoDuplicateMaterials(compositions);

		int totalPercentage = compositions.stream().mapToInt(FabricCompositionRequestDTO::percentage).sum();
		if (totalPercentage != 100) {
			throw new IllegalArgumentException(
					"Total fabric composition percentage must be exactly 100%, but was " + totalPercentage + "%");
		}
	}

	/**
	 * A tabela product_fabric_compositions tem chave primaria (product_id,
	 * material), mas a colecao e uma List — nada deduplica antes do insert. Dois
	 * "Algodao 50%" somam 100% e passavam pela validacao de percentual, para
	 * quebrar no banco e voltar como 409 "A data conflict occurred", que nao diz
	 * qual material esta repetido.
	 *
	 * <p>
	 * Trocar a colecao para Set resolveria o erro e criaria outro: o segundo
	 * material sumiria em silencio e o produto seria salvo com 50% de composicao.
	 */
	private void validateNoDuplicateMaterials(List<FabricCompositionRequestDTO> compositions) {
		Set<String> seen = new LinkedHashSet<>();
		List<String> duplicates = compositions.stream().map(FabricCompositionRequestDTO::material)
				.filter(material -> !seen.add(material)).distinct().toList();

		if (!duplicates.isEmpty()) {
			throw new IllegalArgumentException(
					"Fabric composition cannot repeat the same material: " + String.join(", ", duplicates));
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
