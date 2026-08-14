package com.tm.tsm_atelier.domain.admin.service;

import static com.tm.tsm_atelier.common.builders.ProductRequestDTOBuilder.aProductRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.domain.admin.dto.AuditLogResponseDTO;
import com.tm.tsm_atelier.domain.admin.dto.AuditLogSearchFilter;
import com.tm.tsm_atelier.domain.admin.entity.AuditAction;
import com.tm.tsm_atelier.domain.admin.entity.AuditedEntity;
import com.tm.tsm_atelier.domain.collection.dto.CollectionRequestDTO;
import com.tm.tsm_atelier.domain.collection.enums.DisplayPosition;
import com.tm.tsm_atelier.domain.collection.service.CollectionService;
import com.tm.tsm_atelier.domain.product.dto.AdminProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.CareInstructionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.FabricCompositionRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductColorRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSKURequestDTO;
import com.tm.tsm_atelier.domain.product.dto.StockAdjustmentRequestDTO;
import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import com.tm.tsm_atelier.domain.product.enums.StockChangeReason;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import com.tm.tsm_atelier.domain.product.service.StockService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contra banco de verdade, e pelos serviços reais em vez do AuditService
 * direto.
 *
 * O que precisa ser garantido aqui não é que o serviço de auditoria saiba
 * gravar uma linha — isso é uma chamada de repositório. É que cada alteração
 * administrativa chegue a ele. Um teste com mock provaria que o mock foi
 * chamado onde eu lembrei de chamá-lo, que é exatamente a parte que não precisa
 * de prova.
 *
 * Todas as asserções filtram por entityId, e nenhuma conta linhas do banco
 * inteiro: a base de desenvolvimento tem histórico real, e um containsExactly
 * sobre a tabela toda falharia sem que houvesse defeito nenhum.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.scheduler.order-expiration.enabled=false")
@DisplayName("Admin audit trail")
class AuditTrailIntegrationTest {

	private static final String ACTOR = "maria@atelier.com";

	@Autowired
	private AuditService auditService;

	@Autowired
	private ProductService productService;

	@Autowired
	private StockService stockService;

	@Autowired
	private CollectionService collectionService;

	@PersistenceContext
	private EntityManager entityManager;

	@BeforeEach
	void authenticate() {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(ACTOR, "n/a", List.of()));
	}

	@AfterEach
	void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}

	// ------------------------------------------------------------- produtos

	@Test
	@DisplayName("Should record who created a product")
	void productCreationIsRecorded() {
		AdminProductResponseDTO created = productService.create(aProduct("AUD-CREATE"));
		reload();

		assertThat(historyOf(AuditedEntity.PRODUCT, created.id())).singleElement().satisfies(entry -> {
			assertThat(entry.action()).isEqualTo(AuditAction.CREATED);
			assertThat(entry.actor()).isEqualTo(ACTOR);
			assertThat(entry.createdAt()).isNotNull();
		});
	}

	/**
	 * deleted_at responde quando o produto saiu do ar e nunca quem o tirou. Esta é
	 * a lacuna mais concreta que a tabela fecha.
	 */
	@Test
	@DisplayName("Should record who deleted and who restored a product")
	void deletionAndRestorationAreRecorded() {
		AdminProductResponseDTO created = productService.create(aProduct("AUD-DEL"));
		reload();

		productService.delete(created.id());
		reload();
		productService.restore(created.id());
		reload();

		assertThat(historyOf(AuditedEntity.PRODUCT, created.id())).extracting(AuditLogResponseDTO::action)
				.containsExactlyInAnyOrder(AuditAction.CREATED, AuditAction.DELETED, AuditAction.RESTORED);
	}

	@Test
	@DisplayName("Should record a promotional price alongside the edit that introduced it")
	void promotionalPriceChangeIsRecorded() {
		AdminProductResponseDTO created = productService.create(aProduct("AUD-PROMO"));
		reload();

		productService.update(created.id(), withPromotionalPrice(created, new BigDecimal("149.90")));
		reload();

		assertThat(historyOf(AuditedEntity.PRODUCT, created.id())).extracting(AuditLogResponseDTO::action)
				.containsExactlyInAnyOrder(AuditAction.CREATED, AuditAction.UPDATED,
						AuditAction.PROMOTIONAL_PRICE_CHANGED);

		assertThat(entryFor(created.id(), AuditAction.PROMOTIONAL_PRICE_CHANGED)).satisfies(entry -> {
			// Nulo é "não havia promoção", e precisa continuar distinguível da string
			// "null" depois de gravado.
			assertThat(entry.previousValue()).isNull();
			assertThat(entry.newValue()).isEqualTo("149.90");
		});
	}

	/**
	 * O PUT reenvia o produto inteiro a cada salvamento, então o preço promocional
	 * chega igual em toda edição de descrição. Sem a guarda, o histórico de preço
	 * de uma peça viraria uma coluna de "de 149.90 para 149.90".
	 */
	@Test
	@DisplayName("Should not record a promotional price that did not move")
	void unchangedPromotionalPriceProducesNoRow() {
		AdminProductResponseDTO created = productService.create(aProduct("AUD-NOOP"));
		reload();

		productService.update(created.id(), withPromotionalPrice(created, null));
		reload();

		assertThat(historyOf(AuditedEntity.PRODUCT, created.id())).extracting(AuditLogResponseDTO::action)
				.containsExactlyInAnyOrder(AuditAction.CREATED, AuditAction.UPDATED)
				.doesNotContain(AuditAction.PROMOTIONAL_PRICE_CHANGED);
	}

	// -------------------------------------------------------------- estoque

	/**
	 * O ajuste de estoque é o único evento com motivo declarado pelo operador, e é
	 * ele que torna a tabela consultável para além do "quem": sem o reason, não há
	 * como somar quanto se perdeu em DAMAGE no mês.
	 */
	@Test
	@DisplayName("Should record a stock adjustment with its reason and both quantities")
	void stockAdjustmentIsRecordedWithItsReason() {
		AdminProductResponseDTO created = productService.create(aProduct("AUD-STOCK"));
		Long skuId = created.colors().getFirst().skus().getFirst().id();
		String skuCode = created.colors().getFirst().skus().getFirst().skuCode();
		reload();

		stockService.adjust(skuId, new StockAdjustmentRequestDTO(-3, null, null, StockChangeReason.DAMAGE));
		reload();

		assertThat(historyOf(AuditedEntity.PRODUCT_SKU, skuId)).singleElement().satisfies(entry -> {
			assertThat(entry.action()).isEqualTo(AuditAction.STOCK_ADJUSTED);
			assertThat(entry.previousValue()).isEqualTo("10");
			assertThat(entry.newValue()).isEqualTo("7");
			assertThat(entry.reason()).isEqualTo(StockChangeReason.DAMAGE.name());
			// O SKU pode ser removido depois; a linha precisa continuar legível
			// sozinha, sem depender de um registro que talvez não exista mais.
			assertThat(entry.details()).contains(skuCode);
		});
	}

	// ------------------------------------------------------------- coleções

	/**
	 * O modo entra no registro porque as duas exclusões são irreversíveis de formas
	 * diferentes, e a restauração não desfaz nenhuma das duas.
	 */
	@Test
	@DisplayName("Should record whether a collection delete detached or cascaded")
	void collectionDeleteRecordsItsMode() {
		var collection = collectionService.create(aCollection("AUD-COL"));
		reload();

		collectionService.delete(collection.id(), false);
		reload();

		assertThat(entryFor(AuditedEntity.COLLECTION, collection.id(), AuditAction.DELETED).details())
				.contains("detached");
	}

	@Test
	@DisplayName("Should record a collection restore and how many products stayed behind")
	void collectionRestoreIsRecorded() {
		var collection = collectionService.create(aCollection("AUD-RESTORE"));
		reload();

		collectionService.delete(collection.id(), false);
		reload();
		collectionService.restore(collection.id());
		reload();

		assertThat(entryFor(AuditedEntity.COLLECTION, collection.id(), AuditAction.RESTORED).details())
				.contains("products still deleted");
	}

	// ---------------------------------------------------------------- busca

	@Test
	@DisplayName("Should isolate the history of one record from every other")
	void historyIsScopedToTheRecordAskedFor() {
		AdminProductResponseDTO one = productService.create(aProduct("AUD-SCOPE-A"));
		AdminProductResponseDTO another = productService.create(aProduct("AUD-SCOPE-B"));
		reload();

		assertThat(historyOf(AuditedEntity.PRODUCT, one.id())).hasSize(1);
		assertThat(historyOf(AuditedEntity.PRODUCT, another.id())).hasSize(1);

		// Id exato, e não prefixo: o histórico do produto 4 não pode arrastar o do 42.
		assertThat(historyOf(AuditedEntity.PRODUCT, one.id()))
				.allSatisfy(entry -> assertThat(entry.entityId()).isEqualTo(String.valueOf(one.id())));
	}

	@Test
	@DisplayName("Should find a record by the actor typed as a fragment")
	void actorIsSearchableByFragment() {
		AdminProductResponseDTO created = productService.create(aProduct("AUD-ACTOR"));
		reload();

		var filter = new AuditLogSearchFilter(AuditedEntity.PRODUCT, String.valueOf(created.id()), "MAR", null, null,
				null);

		assertThat(auditService.search(filter, PageRequest.of(0, 20)).getContent()).hasSize(1);
	}

	/**
	 * Intervalo invertido devolveria lista vazia sem erro, e o operador concluiria
	 * que ninguém mexeu em nada naquele período.
	 */
	@Test
	@DisplayName("Should refuse an inverted date range instead of returning nothing")
	void invertedRangeIsRefused() {
		var filter = new AuditLogSearchFilter(null, null, null, null, LocalDate.now(), LocalDate.now().minusDays(7));

		assertThatThrownBy(() -> auditService.search(filter, PageRequest.of(0, 20)))
				.isInstanceOf(BusinessRuleException.class);
	}

	// -------------------------------------------------------------- garantia

	/**
	 * A propagação MANDATORY é o que amarra a linha de auditoria à alteração que
	 * ela descreve: numa transação própria, um rollback do serviço chamador
	 * deixaria registrado um cancelamento que nunca aconteceu.
	 *
	 * NOT_SUPPORTED suspende a transação do teste, que é a única forma de chegar
	 * aqui sem uma — e a exceção é o que impede um serviço novo de gravar auditoria
	 * solta por ter esquecido o @Transactional.
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@DisplayName("Should refuse to record outside of the transaction being audited")
	void recordingOutsideATransactionIsRefused() {
		assertThatThrownBy(() -> auditService.record(AuditedEntity.PRODUCT, 1L, AuditAction.UPDATED))
				.isInstanceOf(IllegalTransactionStateException.class);
	}

	// -------------------------------------------------------------- helpers

	private void reload() {
		entityManager.flush();
		entityManager.clear();
	}

	private List<AuditLogResponseDTO> historyOf(AuditedEntity entityType, Long entityId) {
		var filter = new AuditLogSearchFilter(entityType, String.valueOf(entityId), null, null, null, null);
		return auditService.search(filter, PageRequest.of(0, 50)).getContent();
	}

	private AuditLogResponseDTO entryFor(Long productId, AuditAction action) {
		return entryFor(AuditedEntity.PRODUCT, productId, action);
	}

	private AuditLogResponseDTO entryFor(AuditedEntity entityType, Long entityId, AuditAction action) {
		return historyOf(entityType, entityId).stream().filter(entry -> entry.action() == action).findFirst()
				.orElseThrow(() -> new AssertionError("no " + action + " entry for " + entityType + " " + entityId));
	}

	private ProductRequestDTO aProduct(String prefix) {
		String unique = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);

		return aProductRequest().withName("Vestido " + unique).withCollectionId(null)
				.withColors(List.of(new ProductColorRequestDTO(null, "Azul", "#0000FF", "http://cover.jpg",
						"http://hover.jpg", List.of(), List.of(new ProductSKURequestDTO(null, ProductSize.M, 10)))))
				.build();
	}

	/**
	 * Reconstrói o request a partir da resposta, com stockQuantity nulo nos SKUs
	 * existentes — o formulário de produto recusa estoque desde a MEL-01, e
	 * reenviá-lo aqui faria o teste falhar por um motivo que não é o dele.
	 */
	private ProductRequestDTO withPromotionalPrice(AdminProductResponseDTO product, BigDecimal promotionalPrice) {
		List<ProductColorRequestDTO> colors = product.colors().stream()
				.map(color -> new ProductColorRequestDTO(color.id(), color.colorName(), color.colorHex(),
						color.coverImageUrl(), color.hoverImageUrl(), List.copyOf(color.galleryImages()), color.skus()
								.stream().map(sku -> new ProductSKURequestDTO(sku.id(), sku.size(), null)).toList()))
				.toList();

		return new ProductRequestDTO(product.name(), product.description(), product.fabricCompositions().stream()
				.map(fabric -> new FabricCompositionRequestDTO(fabric.material(), fabric.percentage())).toList(),
				product.careInstructions().stream().map(CareInstructionResponseDTO::instruction).toList(),
				product.price(), promotionalPrice, product.collection() == null ? null : product.collection().id(),
				product.category(), product.targetAudience(), product.active(), product.featured(), colors);
	}

	private CollectionRequestDTO aCollection(String prefix) {
		String unique = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
		return new CollectionRequestDTO("Coleção " + unique, true, "Descrição", "http://banner.jpg", null, null,
				DisplayPosition.NONE, 0, TargetAudience.WOMEN);
	}
}
