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

		// Duas linhas quando o preço mudou, e não uma: "alguém salvou o formulário" e
		// "a promoção mudou" são perguntas diferentes, e só a segunda é filtrável por
		// ação quando se quer o histórico de preço de uma peça.
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
	 * <p>
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

	/** Estoque inicial: é a única gravação de estoque que o cadastro ainda faz. */
	private Integer requireInitialStock(ProductSKURequestDTO skuReq) {
		if (skuReq.stockQuantity() == null) {
			// Pelo tamanho, e nao pelo codigo: o SKU novo so ganha codigo depois
			// desta checagem, e um "New SKU null" nao ajuda ninguem a achar o campo.
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

	/**
	 * Desfaz um {@link #delete(Long)}. Sem isto a exclusão era um caminho só de
	 * ida: a busca do admin mostra produtos removidos, o GET por id funciona, e o
	 * PUT devolvia 404 porque {@code update} filtra por {@code deletedAt IS NULL} —
	 * o admin via o item na lista, abria o formulário e não conseguia salvar.
	 *
	 * <p>
	 * O produto volta <strong>inativo</strong>. Restaurar é recuperar o cadastro;
	 * colocá-lo de volta na vitrine é uma segunda decisão, e juntar as duas faria
	 * um clique republicar um produto sem que ninguém revisasse preço e estoque.
	 */
	@Transactional
	@CacheEvict(value = {CacheNames.CATALOG_PRODUCTS, CacheNames.CATALOG_SLUG}, allEntries = true)
	public AdminProductResponseDTO restore(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));

		if (product.getDeletedAt() == null) {
			throw new BusinessRuleException("Product " + id + " is not deleted.");
		}

		LocalDateTime deletedAt = product.getDeletedAt();

		// Antes de qualquer UPDATE: o índice parcial de sku_code só admite um SKU
		// vivo por código, e um cadastro criado depois da exclusão pode ter tomado
		// o lugar. Sem esta checagem o erro voltaria como 409 genérico, sem dizer
		// qual código está ocupado.
		List<String> blockedCodes = skuRepository.findSkuCodesBlockingRestore(id, deletedAt);
		if (!blockedCodes.isEmpty()) {
			throw new EntityAlreadyExistsException("SKU", String.join(", ", blockedCodes));
		}

		skuRepository.restoreSkusOfProduct(id, deletedAt);
		colorRepository.restoreColorsOfProduct(id, deletedAt);

		// Releitura obrigatória: as consultas acima limpam o contexto de persistência
		// para que cores e SKUs sejam carregados já sem deleted_at.
		Product restored = productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product", id));
		restored.setDeletedAt(null);

		auditService.record(AuditedEntity.PRODUCT, id, AuditAction.RESTORED);

		return productMapper.toAdminResponse(restored);
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

		return toSummaryPage(productRepository.findAll(spec, pageable), productMapper::toSummary);
	}

	@Transactional(readOnly = true)
	public Page<AdminProductSummaryDTO> searchAdmin(ProductSearchFilter filter, Pageable pageable) {
		return toSummaryPage(productRepository.findAll(Specification.where(matches(filter)), pageable),
				productMapper::toAdminSummary);
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

	/**
	 * As duas buscas percorrem os mesmos produtos e diferem apenas no card que
	 * produzem: o do admin carrega deletedAt, porque a listagem dele e a unica que
	 * enxerga removidos e oferece a restauracao.
	 */
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

	/**
	 * O código do SKU, gerado e não digitado. É interno — não vem de ERP, não vai
	 * para etiqueta impressa — então inventá-lo nunca foi decisão do admin, e
	 * cobrá-la dele custava quinze campos por produto e um 409 que só aparecia
	 * depois do formulário inteiro preenchido.
	 *
	 * <p>
	 * Opaco de propósito. No checkout o código é copiado para dentro do pedido
	 * ({@code OrderItem.skuCode}) e a partir dali é imutável; mas tudo que a
	 * aplicação conhece no instante em que o SKU nasce — nome do produto, nome da
	 * cor, tamanho — pode ser editado depois. Um código descritivo viraria uma
	 * afirmação falsa no primeiro rename, gravada no banco e repetida em todo
	 * pedido antigo. O slug tem o mesmo problema e o resolve com 301; um código
	 * congelado num pedido não tem para onde redirecionar.
	 *
	 * <p>
	 * A sequência é única por construção, o que dispensa consulta de verificação e
	 * laço de retentativa — a máquina que a geração veio justamente eliminar. E
	 * como número nunca se repete, SKU removido e restaurado não colide com
	 * ninguém.
	 */
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

	/**
	 * A rota pública por slug. Até aqui este método não tinha chamador: o
	 * controller extraía um id do texto e chamava {@code findById}, então o cache
	 * {@code catalog_slug} nunca era populado e todos os {@code @CacheEvict} que o
	 * citam invalidavam um cache vazio.
	 *
	 * <p>
	 * A chave é o slug puro. Ela já levou o prefixo {@code product:} para não
	 * disputar entrada com a coleção, que dividia este cache; a divisão em si era o
	 * defeito — um cache do Redis tem um serializer de valor por nome, e o desta
	 * entrada é {@code ProductResponseDTO}. Coleção agora tem cache próprio.
	 */
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
	 *
	 * <p>
	 * A checagem só passou a ser completa quando o material virou {@link Material}.
	 * Enquanto era texto livre ela comparava string exata, então "Algodao" e
	 * "Algodão" eram dois materiais para ela e para a chave primaria — a mesma
	 * fibra entrava duas vezes sem que nada reclamasse. Com o enum, "o mesmo
	 * material" tem uma unica resposta possivel.
	 */
	private void validateNoDuplicateMaterials(List<FabricCompositionRequestDTO> compositions) {
		Set<Material> seen = EnumSet.noneOf(Material.class);
		List<String> duplicates = compositions.stream().map(FabricCompositionRequestDTO::material)
				.filter(material -> !seen.add(material)).distinct().map(Material::getLabel).toList();

		if (!duplicates.isEmpty()) {
			throw new BusinessRuleException(
					"Fabric composition cannot repeat the same material: " + String.join(", ", duplicates));
		}
	}

	/**
	 * A contrapartida de {@link #validateFabricComposition}: composicao tem a soma
	 * 100%, cuidado tem a nao-contradicao. Sao a mesma classe de regra — o enum
	 * garante que cada valor existe, e nada nele garante que o conjunto faz
	 * sentido.
	 *
	 * <p>
	 * Sem esta checagem, um multi-select deixa o admin marcar "Nao lavar" e "Lavar
	 * a mao" na mesma peca, ou "Nao passar" com "Passar em temperatura baixa". E o
	 * erro mais caro dos dois: um "Lavar a mao" sem acento salta aos olhos na
	 * vitrine, enquanto um par contraditorio le como instrucao legitima e estraga a
	 * peca do cliente.
	 *
	 * <p>
	 * Repetir a <em>mesma</em> constante nao e contradicao, e o Set da entidade ja
	 * a descarta — so o conflito entre duas instrucoes do mesmo eixo e recusado.
	 */
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
