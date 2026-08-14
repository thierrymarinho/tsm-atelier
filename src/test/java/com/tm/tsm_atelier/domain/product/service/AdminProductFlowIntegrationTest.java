package com.tm.tsm_atelier.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.common.exception.custom.ResourceNotFoundException;
import com.tm.tsm_atelier.domain.product.dto.AdminProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.CareInstructionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.FabricCompositionRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductColorRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSKURequestDTO;
import com.tm.tsm_atelier.domain.product.enums.CareInstruction;
import com.tm.tsm_atelier.domain.product.enums.Category;
import com.tm.tsm_atelier.domain.product.enums.Material;
import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import com.tm.tsm_atelier.domain.product.enums.TargetAudience;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fluxo de edição do admin contra banco de verdade.
 *
 * <p>
 * O {@code flush()} + {@code clear()} entre os passos não é cerimônia: é o que
 * faz o teste valer alguma coisa. Todos os defeitos cobertos aqui só aparecem
 * na <em>releitura</em> — enquanto tudo acontece no mesmo contexto de
 * persistência, a coleção em memória já reflete a alteração e o teste passaria
 * com ou sem a correção.
 */
@SpringBootTest
@Transactional
@DisplayName("Admin product editing flow")
class AdminProductFlowIntegrationTest {

	@Autowired
	private ProductService productService;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("Every SKU is born with its own generated code")
	void skuCodesAreGeneratedAndDistinct() {
		AdminProductResponseDTO created = productService.create(aProductWith("GN"));
		reload();

		List<String> codes = created.colors().stream().flatMap(colour -> colour.skus().stream())
				.map(sku -> sku.skuCode()).toList();

		assertThat(codes).hasSize(2).doesNotHaveDuplicates().allMatch(code -> code.matches("TSM-\\d{6}"));
	}

	/**
	 * A garantia que sustenta o snapshot do pedido: no checkout o código é copiado
	 * para dentro do {@code OrderItem}, e se uma edição do cadastro o reescrevesse,
	 * catálogo e histórico passariam a discordar sobre a mesma peça. É por isso que
	 * o código é opaco — se ele carregasse o nome do produto, este teste seria
	 * impossível de satisfazer sem mentir.
	 */
	@Test
	@DisplayName("A SKU keeps its code when the product is renamed")
	void skuCodeSurvivesARename() {
		AdminProductResponseDTO created = productService.create(aProductWith("RN"));
		reload();

		List<String> before = codesOf(created);

		ProductRequestDTO renamed = withName(toRequest(created, colours -> {
		}), "Vestido Rebatizado RN");
		productService.update(created.id(), renamed);
		reload();

		AdminProductResponseDTO reread = productService.findAdminById(created.id());

		assertThat(reread.name()).isEqualTo("Vestido Rebatizado RN");
		assertThat(codesOf(reread)).as("o código está congelado em todo pedido que já levou este SKU")
				.isEqualTo(before);
	}

	@Test
	@DisplayName("A SKU added in the PUT gets a fresh code, and the existing ones keep theirs")
	void addedSkuGetsItsOwnCode() {
		AdminProductResponseDTO created = productService.create(aProductWith("AD"));
		reload();

		List<String> before = codesOf(created);

		ProductRequestDTO withExtraSize = toRequest(created, colours -> {
			ProductColorRequestDTO first = colours.getFirst();
			List<ProductSKURequestDTO> skus = new ArrayList<>(first.skus());
			skus.add(new ProductSKURequestDTO(null, ProductSize.GG, 7));
			colours.set(0, new ProductColorRequestDTO(first.id(), first.colorName(), first.colorHex(),
					first.coverImageUrl(), first.hoverImageUrl(), first.galleryImages(), skus));
		});

		productService.update(created.id(), withExtraSize);
		reload();

		List<String> after = codesOf(productService.findAdminById(created.id()));

		assertThat(after).hasSize(3).doesNotHaveDuplicates().containsAll(before);
	}

	@Test
	@DisplayName("A colour removed in the PUT stays removed on the next read")
	void removedColourStaysRemoved() {
		AdminProductResponseDTO created = productService.create(aProductWith("RC"));
		reload();

		ProductRequestDTO withoutSecondColour = toRequest(created, colours -> colours.remove(1));
		productService.update(created.id(), withoutSecondColour);
		reload();

		AdminProductResponseDTO reread = productService.findAdminById(created.id());

		assertThat(reread.colors()).hasSize(1);
		assertThat(reread.colors().getFirst().colorName()).isEqualTo("Azul");

		// A segunda metade do defeito: a cor removida voltava sem SKU nenhum, e o
		// @NotEmpty do request recusava todo salvamento seguinte com 422.
		assertThat(reread.colors()).allSatisfy(colour -> assertThat(colour.skus()).isNotEmpty());

		assertThatCode(() -> productService.update(created.id(), toRequest(reread, colours -> {
		}))).doesNotThrowAnyException();
	}

	/**
	 * O round-trip puro: carregar o produto e devolvê-lo sem alterar nada tem que
	 * passar. Parece trivial e é o teste que pega o BUG-01 — a cor removida voltava
	 * no GET sem SKU nenhum, e o {@code @NotEmpty} do request recusava com 422 um
	 * salvamento que não mudava coisa alguma.
	 *
	 * <p>
	 * Vale como regressão de contrato mais ampla: qualquer campo que o GET devolva
	 * e o PUT não aceite de volta quebra aqui.
	 */
	@Test
	@DisplayName("Loading a product and saving it back unchanged is accepted")
	void unchangedRoundTripIsAccepted() {
		AdminProductResponseDTO created = productService.create(aProductWith("RT"));
		reload();

		AdminProductResponseDTO loaded = productService.findAdminById(created.id());

		assertThatCode(() -> productService.update(created.id(), toRequest(loaded, colours -> {
		}))).doesNotThrowAnyException();
		reload();

		AdminProductResponseDTO reread = productService.findAdminById(created.id());

		assertThat(reread.colors()).hasSize(2);
		assertThat(reread.colors()).allSatisfy(colour -> assertThat(colour.skus()).hasSize(1));
		assertThat(reread.name()).isEqualTo(loaded.name());
		assertThat(reread.price()).isEqualByComparingTo(loaded.price());
	}

	/**
	 * O caso que motivava exigir a versão de cada SKU no PUT: o formulário fica
	 * aberto, clientes compram, e o salvamento devolvia ao estoque as unidades
	 * vendidas nesse intervalo.
	 *
	 * <p>
	 * A defesa anterior era recusar o salvamento com 409 — correta, e cara: uma
	 * correção de descrição morria por causa de uma venda que não tinha nada a ver
	 * com ela. Com o estoque fora deste payload a corrida deixa de existir, e o
	 * mesmo cenário agora passa <em>sem</em> desfazer a venda.
	 */
	@Test
	@DisplayName("An edit saved while the product was selling goes through, and the sale stands")
	void anEditMadeWhileStockMovedKeepsTheSale() {
		AdminProductResponseDTO created = productService.create(aProductWith("SV"));
		reload();

		// O formulário do admin foi carregado aqui, com estoque 10.
		ProductRequestDTO openForm = toRequest(created, colours -> {
		});
		String soldSku = created.colors().getFirst().skus().getFirst().skuCode();

		// Enquanto ele fica aberto, um cliente compra três unidades.
		entityManager.createNativeQuery(
				"UPDATE product_skus SET stock_quantity = stock_quantity - 3, version = version + 1 WHERE sku_code = :code")
				.setParameter("code", soldSku).executeUpdate();
		reload();

		assertThatCode(() -> productService.update(created.id(), openForm)).doesNotThrowAnyException();
		reload();

		assertThat(stockOf(created.id(), soldSku)).as("the PUT no longer writes stock, so the sale survives it")
				.isEqualTo(7);
	}

	@Test
	@DisplayName("Sending stock for an existing sku is refused, and the message names the right endpoint")
	void stockSentForAnExistingSkuIsRefused() {
		AdminProductResponseDTO created = productService.create(aProductWith("SR"));
		reload();

		ProductRequestDTO echoingStock = withSkuStock(toRequest(created, colours -> {
		}), 99);

		// Recusar, e não ignorar: um 200 para uma alteração que não aconteceu seria
		// pior do que qualquer erro.
		assertThatThrownBy(() -> productService.update(created.id(), echoingStock))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("/api/v1/admin/skus/")
				.hasMessageContaining("/stock");
	}

	@Test
	@DisplayName("A new sku still carries its initial stock")
	void newSkuStillNeedsInitialStock() {
		ProductRequestDTO withoutStock = withSkuStock(aProductWith("IS"), null);

		assertThatThrownBy(() -> productService.create(withoutStock)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("initial stockQuantity");
	}

	@Test
	@DisplayName("A deleted product can be restored, and comes back inactive")
	void deletedProductCanBeRestored() {
		AdminProductResponseDTO created = productService.create(aProductWith("RE"));
		reload();

		productService.delete(created.id());
		reload();

		// Antes da rota de restauração isto era o beco sem saída: a busca do admin
		// listava o produto, o GET por id funcionava e o PUT devolvia 404.
		assertThatThrownBy(() -> productService.update(created.id(), toRequest(created, colours -> {
		}))).isInstanceOf(ResourceNotFoundException.class);

		AdminProductResponseDTO restored = productService.restore(created.id());

		assertThat(restored.deletedAt()).isNull();
		assertThat(restored.colors()).hasSize(2);
		assertThat(restored.colors()).allSatisfy(colour -> assertThat(colour.skus()).hasSize(1));
		assertThat(restored.active()).as("restoring recovers the record; republishing is a separate decision")
				.isFalse();
	}

	@Test
	@DisplayName("Restoring does not resurrect colours the admin had removed earlier")
	void restoreLeavesEarlierRemovalsAlone() {
		AdminProductResponseDTO created = productService.create(aProductWith("RL"));
		reload();

		productService.update(created.id(), toRequest(created, colours -> colours.remove(1)));
		reload();

		productService.delete(created.id());
		reload();

		AdminProductResponseDTO restored = productService.restore(created.id());

		assertThat(restored.colors()).hasSize(1);
	}

	// ---------------------------------------------------------------- helpers

	/**
	 * Força a próxima leitura a ir ao banco. Sem isso os testes acima leriam a
	 * mesma instância que acabaram de alterar.
	 */
	private void reload() {
		entityManager.flush();
		entityManager.clear();
	}

	private ProductRequestDTO aProductWith(String suffix) {
		return new ProductRequestDTO("Vestido de Teste " + suffix, "Descrição",
				List.of(new FabricCompositionRequestDTO(Material.COTTON, 100)),
				List.of(CareInstruction.MACHINE_WASH_COLD), new BigDecimal("200.00"), null, null, Category.DRESSES,
				TargetAudience.WOMEN, true, false, twoColours());
	}

	private List<ProductColorRequestDTO> twoColours() {
		return List.of(colour("Azul", "#0000FF"), colour("Verde", "#00FF00"));
	}

	private ProductColorRequestDTO colour(String name, String hex) {
		return new ProductColorRequestDTO(null, name, hex, "http://cover.jpg", "http://hover.jpg", List.of(),
				List.of(new ProductSKURequestDTO(null, ProductSize.M, 10)));
	}

	/**
	 * Reconstrói o payload do PUT a partir do que o GET devolveu, que é exatamente
	 * o que o formulário do admin faz. O {@code mutation} representa a edição do
	 * usuário sobre a lista de cores.
	 *
	 * <p>
	 * O estoque é deixado de fora de propósito: SKU existente não o aceita mais, e
	 * é assim que o front precisa montar o payload.
	 */
	private ProductRequestDTO toRequest(AdminProductResponseDTO response,
			java.util.function.Consumer<List<ProductColorRequestDTO>> mutation) {

		List<ProductColorRequestDTO> colours = new ArrayList<>(response
				.colors().stream().map(
						colour -> new ProductColorRequestDTO(
								colour.id(), colour.colorName(), colour.colorHex(), colour.coverImageUrl(),
								colour.hoverImageUrl(), colour.galleryImages(), colour.skus().stream()
										.map(sku -> new ProductSKURequestDTO(sku.id(), sku.size(), null)).toList()))
				.toList());

		mutation.accept(colours);

		return new ProductRequestDTO(response.name(), response.description(), response.fabricCompositions().stream()
				.map(fabric -> new FabricCompositionRequestDTO(fabric.material(), fabric.percentage())).toList(),
				response.careInstructions().stream().map(CareInstructionResponseDTO::instruction).toList(),
				response.price(), response.promotionalPrice(),
				response.collection() == null ? null : response.collection().id(), response.category(),
				response.targetAudience(), response.active(), response.featured(), colours);
	}

	private ProductRequestDTO withSkuStock(ProductRequestDTO request, Integer stockQuantity) {
		return withColours(request, request.colors().stream()
				.map(colour -> new ProductColorRequestDTO(colour.id(), colour.colorName(), colour.colorHex(),
						colour.coverImageUrl(), colour.hoverImageUrl(), colour.galleryImages(), colour.skus().stream()
								.map(sku -> new ProductSKURequestDTO(sku.id(), sku.size(), stockQuantity)).toList()))
				.toList());
	}

	private List<String> codesOf(AdminProductResponseDTO response) {
		return response.colors().stream().flatMap(colour -> colour.skus().stream()).map(sku -> sku.skuCode()).toList();
	}

	private ProductRequestDTO withName(ProductRequestDTO request, String name) {
		return new ProductRequestDTO(name, request.description(), request.fabricCompositions(),
				request.careInstructions(), request.price(), request.promotionalPrice(), request.collectionId(),
				request.category(), request.targetAudience(), request.active(), request.featured(), request.colors());
	}

	private Integer stockOf(Long productId, String skuCode) {
		return productService.findAdminById(productId).colors().stream().flatMap(colour -> colour.skus().stream())
				.filter(sku -> sku.skuCode().equals(skuCode)).findFirst().orElseThrow().stockQuantity();
	}

	private ProductRequestDTO withColours(ProductRequestDTO request, List<ProductColorRequestDTO> colours) {
		return new ProductRequestDTO(request.name(), request.description(), request.fabricCompositions(),
				request.careInstructions(), request.price(), request.promotionalPrice(), request.collectionId(),
				request.category(), request.targetAudience(), request.active(), request.featured(), colours);
	}
}
