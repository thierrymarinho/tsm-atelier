package com.tm.tsm_atelier.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.common.exception.custom.StaleResourceException;
import com.tm.tsm_atelier.domain.admin.dto.DashboardResponseDTO;
import com.tm.tsm_atelier.domain.admin.dto.LowStockSkuDTO;
import com.tm.tsm_atelier.domain.collection.repository.CollectionRepository;
import com.tm.tsm_atelier.domain.order.entity.Order;
import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.entity.ShippingAddress;
import com.tm.tsm_atelier.domain.order.repository.OrderRepository;
import com.tm.tsm_atelier.domain.product.dto.ProductRequestDTO;
import com.tm.tsm_atelier.domain.product.dto.StockAdjustmentRequestDTO;
import com.tm.tsm_atelier.domain.product.enums.StockChangeReason;
import com.tm.tsm_atelier.domain.product.service.ProductService;
import com.tm.tsm_atelier.domain.product.service.StockService;
import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contra banco de verdade: o que este serviço faz é escrever consultas de
 * agregação, e uma agregação testada com mock só prova que o mock foi chamado.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.scheduler.order-expiration.enabled=false")
@DisplayName("Admin dashboard")
class DashboardServiceTest {

	@Autowired
	private DashboardService dashboardService;

	@Autowired
	private ProductService productService;

	@Autowired
	private StockService stockService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CollectionRepository collectionRepository;

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * O agrupamento do banco só devolve linhas que existem. Sem completar o mapa,
	 * um status sem nenhum pedido sumiria da resposta, e a interface trataria
	 * "chave ausente" e "zero pedidos" como o mesmo caso — que é como um contador
	 * desaparece da tela sem ninguém notar.
	 */
	@Test
	@DisplayName("Should report every status, including the ones with no orders")
	void everyStatusIsPresent() {
		assertThat(dashboardService.summary(5).ordersByStatus()).containsOnlyKeys(OrderStatus.values());
	}

	@Test
	@DisplayName("Should count an order under the status it is in")
	void countsByStatus() {
		long before = dashboardService.summary(5).ordersByStatus().get(OrderStatus.SHIPPED);

		persistOrder(OrderStatus.SHIPPED, new BigDecimal("100.00"), LocalDateTime.now());
		reload();

		assertThat(dashboardService.summary(5).ordersByStatus().get(OrderStatus.SHIPPED)).isEqualTo(before + 1);
	}

	/**
	 * Faturamento é valor de pedido que a loja pode reconhecer. PENDING_PAYMENT
	 * ainda não é dinheiro, PAYMENT_FAILED nunca foi e CANCELLED saiu da conta —
	 * somar os três inflaria o número mais visível do painel.
	 */
	@Test
	@DisplayName("Should only count paid, shipped and delivered orders as revenue")
	void revenueIgnoresOrdersThatAreNotMoney() {
		BigDecimal before = dashboardService.summary(5).revenue().today();

		persistOrder(OrderStatus.PAID, new BigDecimal("100.00"), LocalDateTime.now());
		persistOrder(OrderStatus.PENDING_PAYMENT, new BigDecimal("999.00"), LocalDateTime.now());
		persistOrder(OrderStatus.CANCELLED, new BigDecimal("999.00"), LocalDateTime.now());
		persistOrder(OrderStatus.PAYMENT_FAILED, new BigDecimal("999.00"), LocalDateTime.now());
		reload();

		assertThat(dashboardService.summary(5).revenue().today())
				.isEqualByComparingTo(before.add(new BigDecimal("100.00")));
	}

	/**
	 * As janelas contam dias inteiros a partir da meia-noite. Um pedido de ontem
	 * não pode entrar em "hoje", e tem que entrar em "últimos 7 dias".
	 */
	@Test
	@DisplayName("Should place an order from yesterday outside today and inside the week")
	void windowsCountWholeDays() {
		var before = dashboardService.summary(5).revenue();

		persistOrder(OrderStatus.PAID, new BigDecimal("50.00"), LocalDateTime.now().minusDays(1));
		reload();

		var after = dashboardService.summary(5).revenue();

		assertThat(after.today()).isEqualByComparingTo(before.today());
		assertThat(after.last7Days()).isEqualByComparingTo(before.last7Days().add(new BigDecimal("50.00")));
		assertThat(after.last30Days()).isEqualByComparingTo(before.last30Days().add(new BigDecimal("50.00")));
	}

	/**
	 * As asserções são sobre o total, e não sobre a amostra: a base de
	 * desenvolvimento já tem mais de vinte SKUs com estoque baixo, então um SKU
	 * novo legitimamente não aparece nas vinte primeiras linhas. A primeira versão
	 * deste teste procurava o id na amostra e falhava por isso — sem que houvesse
	 * defeito nenhum no código.
	 */
	@Test
	@DisplayName("Should count a sku that fell below the threshold")
	void countsLowStock() {
		long before = dashboardService.summary(5).lowStockCount();

		aProductWithStock("DASH-LOW", 2);
		reload();

		assertThat(dashboardService.summary(5).lowStockCount()).isEqualTo(before + 1);
	}

	@Test
	@DisplayName("Should leave a well-stocked sku out of the alert")
	void ignoresHealthyStock() {
		long before = dashboardService.summary(5).lowStockCount();

		aProductWithStock("DASH-OK", 50);
		reload();

		assertThat(dashboardService.summary(5).lowStockCount()).isEqualTo(before);
	}

	/**
	 * O alerta existe para avisar que a loja está prestes a perder venda. Um
	 * produto fora da vitrine não perde venda nenhuma — incluí-lo transformaria o
	 * painel num inventário de rascunhos, e o número de alertas deixaria de
	 * significar urgência.
	 */
	@Test
	@DisplayName("Should ignore low stock on a product that is not for sale")
	void ignoresProductsOutOfTheShopWindow() {
		long before = dashboardService.summary(5).lowStockCount();

		Long deletedProductSku = aProductWithStock("DASH-DEL", 1);
		Long inactiveProductSku = aProductWithStock("DASH-INACTIVE", 1);
		reload();

		assertThat(dashboardService.summary(5).lowStockCount()).as("os dois entram enquanto estão à venda")
				.isEqualTo(before + 2);

		productService.delete(productIdOfSku(deletedProductSku));
		deactivate(productIdOfSku(inactiveProductSku));
		reload();

		assertThat(dashboardService.summary(5).lowStockCount()).isEqualTo(before);
	}

	/**
	 * A amostra é um alerta de tela, não um relatório. O total acompanha à parte
	 * para a interface poder dizer "20 de 47" em vez de insinuar que são 20.
	 */
	@Test
	@DisplayName("Should cap the sample and report the real total alongside it")
	void sampleIsCappedAndTotalTravelsWithIt() {
		DashboardResponseDTO summary = dashboardService.summary(5);

		assertThat(summary.lowStock()).hasSizeLessThanOrEqualTo(20);
		assertThat(summary.lowStockCount()).isGreaterThanOrEqualTo(summary.lowStock().size());
	}

	@Test
	@DisplayName("Should describe each alert well enough to act on it")
	void alertsAreActionable() {
		aProductWithStock("DASH-SHAPE", 0);
		reload();

		assertThat(dashboardService.summary(5).lowStock()).isNotEmpty().allSatisfy(sku -> {
			assertThat(sku.stockQuantity()).isLessThanOrEqualTo(5);
			// O código do SKU sozinho obriga o operador a ir descobrir de que peça se
			// trata; o skuId é o que a linha usa para chamar o PATCH de estoque.
			assertThat(sku.skuId()).isNotNull();
			assertThat(sku.productName()).isNotBlank();
			assertThat(sku.colorName()).isNotBlank();
			assertThat(sku.size()).isNotNull();
			// Sem a versão a linha só oferece delta, e a contagem física — o gesto de
			// quem confere prateleira — fica fora do alcance da única tela que lista
			// justamente os SKUs que serão conferidos.
			assertThat(sku.version()).isNotNull();
		});
	}

	/**
	 * Que o campo exista não basta: ele só serve se for a mesma versão que
	 * StockService exige para aceitar uma contagem. Um número plausível porém de
	 * outra origem passaria em qualquer asserção de forma e falharia com 409 na
	 * primeira contagem real.
	 */
	@Test
	@DisplayName("Should hand out a version the physical count is allowed to use")
	void versionTravelsUsableByTheCount() {
		// Semeia só para a lista nunca vir vazia; o alerta conferido é o primeiro que
		// a página trouxer, seja qual for. Procurar o SKU semeado dentro da página
		// seria frágil: a lista sobe do menor estoque para o maior e cabem vinte,
		// então em base com muitos zerados ele simplesmente não aparece.
		aProductWithStock("DASH-COUNT", 2);
		reload();

		LowStockSkuDTO alert = dashboardService.summary(5).lowStock().stream().findFirst().orElseThrow();
		int counted = alert.stockQuantity() + 3;

		var applied = stockService.adjust(alert.skuId(),
				new StockAdjustmentRequestDTO(null, counted, alert.version(), StockChangeReason.INVENTORY_COUNT));

		assertThat(applied.stockQuantity()).isEqualTo(counted);
		assertThat(applied.version()).isGreaterThan(alert.version());

		// A versão devolvida já veio incrementada, então repetir a contagem com a
		// versão da listagem tem que ser recusada — é essa recusa que a tela
		// transforma em "o sistema diz 7, você contou 5".
		assertThatThrownBy(() -> stockService.adjust(alert.skuId(),
				new StockAdjustmentRequestDTO(null, counted, alert.version(), StockChangeReason.INVENTORY_COUNT)))
				.isInstanceOf(StaleResourceException.class);
	}

	@Test
	@DisplayName("Should refuse a negative or absurd threshold")
	void refusesNonsenseThresholds() {
		assertThatThrownBy(() -> dashboardService.summary(-1)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> dashboardService.summary(1_000_001)).isInstanceOf(BusinessRuleException.class);
	}

	/**
	 * Sem paginação a amostra era um beco sem saída: a resposta anunciava "20 de
	 * 37" e não havia caminho até os outros dezessete. Mexer no limiar não resolve,
	 * porque a lista sobe do menor estoque para o maior — os que faltam são os de
	 * estoque mais alto, e não existe piso para excluir os já vistos.
	 */
	@Test
	@DisplayName("Should reach the alerts beyond the first page")
	void paginatesTheLowStockSample() {
		DashboardResponseDTO first = dashboardService.summary(5, 0);
		assumeTrue(first.lowStockCount() > first.lowStockPageSize(),
				"a base precisa de mais de uma página de estoque baixo para este teste dizer algo");

		DashboardResponseDTO second = dashboardService.summary(5, 1);

		assertThat(second.lowStock()).isNotEmpty();
		assertThat(second.lowStockPage()).isEqualTo(1);
		// Mesmo total nas duas: paginar recorta a amostra, não o universo.
		assertThat(second.lowStockCount()).isEqualTo(first.lowStockCount());

		Set<Long> firstIds = first.lowStock().stream().map(LowStockSkuDTO::skuId).collect(Collectors.toSet());
		assertThat(second.lowStock()).extracting(LowStockSkuDTO::skuId).doesNotContainAnyElementsOf(firstIds);
	}

	@Test
	@DisplayName("Should hand back an empty page past the end instead of failing")
	void pageBeyondTheEndIsEmpty() {
		DashboardResponseDTO summary = dashboardService.summary(5, 10_000);

		assertThat(summary.lowStock()).isEmpty();
		// O total continua verdadeiro — é o que impede a tela de concluir que o
		// alerta acabou só porque a página pedida passou do fim.
		assertThat(summary.lowStockCount()).isEqualTo(dashboardService.summary(5, 0).lowStockCount());
	}

	@Test
	@DisplayName("Should refuse a negative page")
	void refusesNegativePage() {
		// Sem a guarda no serviço isto seria IllegalArgumentException do PageRequest,
		// que sobe como 500 — erro de quem chamou apresentado como falha do servidor.
		assertThatThrownBy(() -> dashboardService.summary(5, -1)).isInstanceOf(BusinessRuleException.class);
	}

	// ---------------------------------------------------------------- helpers

	private void reload() {
		entityManager.flush();
		entityManager.clear();
	}

	private Long aProductWithStock(String prefix, int stock) {
		var created = productService.create(new ProductRequestDTO("Vestido " + prefix, "Descrição",
				List.of(new com.tm.tsm_atelier.domain.product.dto.FabricCompositionRequestDTO(
						com.tm.tsm_atelier.domain.product.enums.Material.COTTON, 100)),
				List.of(com.tm.tsm_atelier.domain.product.enums.CareInstruction.MACHINE_WASH_COLD),
				new BigDecimal("200.00"), null, null, com.tm.tsm_atelier.domain.product.enums.Category.DRESSES,
				com.tm.tsm_atelier.domain.product.enums.TargetAudience.WOMEN, true, false,
				List.of(new com.tm.tsm_atelier.domain.product.dto.ProductColorRequestDTO(null, "Azul", "#0000FF",
						"http://cover.jpg", "http://hover.jpg", List.of(),
						List.of(new com.tm.tsm_atelier.domain.product.dto.ProductSKURequestDTO(null,
								com.tm.tsm_atelier.domain.product.enums.ProductSize.M, stock))))));

		return created.colors().getFirst().skus().getFirst().id();
	}

	private Long productIdOfSku(Long skuId) {
		return ((Number) entityManager.createNativeQuery("""
				SELECT c.product_id FROM product_skus s JOIN product_colors c ON c.id = s.product_color_id
				WHERE s.id = :id
				""").setParameter("id", skuId).getSingleResult()).longValue();
	}

	private void deactivate(Long productId) {
		entityManager.createNativeQuery("UPDATE products SET active = false WHERE id = :id")
				.setParameter("id", productId).executeUpdate();
	}

	private Long persistOrder(OrderStatus status, BigDecimal totalAmount, LocalDateTime createdAt) {
		User user = userRepository
				.save(User.builder().email("dash-" + java.util.UUID.randomUUID() + "@example.com").firstName("Dash")
						.lastName("Board").password("irrelevant").role(Role.CUSTOMER).emailVerified(true).build());

		Order order = orderRepository.saveAndFlush(Order.builder().user(user).status(status).totalAmount(totalAmount)
				.shippingFee(BigDecimal.ZERO).expiresAt(createdAt.plusMinutes(30))
				.shippingAddress(ShippingAddress.builder().street("Rua A").number("1").neighborhood("Centro")
						.city("São Paulo").state("SP").postalCode("01001000").build())
				.build());

		// createdAt é preenchido pelo auditing e não aceita valor no insert.
		entityManager.createNativeQuery("UPDATE orders SET created_at = :createdAt WHERE id = :id")
				.setParameter("createdAt", createdAt).setParameter("id", order.getId()).executeUpdate();

		return order.getId();
	}
}
