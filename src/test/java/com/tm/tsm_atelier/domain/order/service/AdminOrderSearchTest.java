package com.tm.tsm_atelier.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tm.tsm_atelier.common.exception.custom.BusinessRuleException;
import com.tm.tsm_atelier.domain.order.dto.AdminOrderResponseDTO;
import com.tm.tsm_atelier.domain.order.dto.OrderSearchFilter;
import com.tm.tsm_atelier.domain.order.entity.Order;
import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.entity.ShippingAddress;
import com.tm.tsm_atelier.domain.order.repository.OrderRepository;
import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Busca da listagem de pedidos contra banco de verdade — filtro montado com
 * Specification só vale alguma coisa quando o SQL de fato roda.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.scheduler.order-expiration.enabled=false")
@DisplayName("Admin order search")
class AdminOrderSearchTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private UserRepository userRepository;

	@PersistenceContext
	private EntityManager entityManager;

	private Long mariaOrderId;
	private Long joaoOrderId;

	@BeforeEach
	void seed() {
		User maria = persistUser("maria.busca@example.com", "Maria", "Silva");
		User joao = persistUser("joao.busca@example.com", "João", "Pereira");

		mariaOrderId = persistOrder(maria, OrderStatus.PENDING_PAYMENT, LocalDateTime.now().minusDays(10));
		joaoOrderId = persistOrder(joao, OrderStatus.SHIPPED, LocalDateTime.now().minusDays(1));

		entityManager.flush();
		entityManager.clear();
	}

	@Test
	@DisplayName("Should find an order by a fragment of the customer email")
	void findsByEmailFragment() {
		assertThat(idsMatching(new OrderSearchFilter(null, "maria.busca", null, null))).containsExactly(mariaOrderId);
	}

	@Test
	@DisplayName("Should find an order by the customer name, in any case")
	void findsByName() {
		assertThat(idsMatching(new OrderSearchFilter(null, "pereira", null, null))).containsExactly(joaoOrderId);
	}

	/**
	 * O operador que digita "12" quer o pedido 12, e não os pedidos 12, 112 e 120 —
	 * por isso o id casa por igualdade, e não como texto.
	 */
	@Test
	@DisplayName("Should match the order id exactly, not as a substring")
	void matchesOrderIdExactly() {
		assertThat(idsMatching(new OrderSearchFilter(null, mariaOrderId.toString(), null, null)))
				.contains(mariaOrderId);

		assertThat(idsMatching(new OrderSearchFilter(null, mariaOrderId + "0", null, null)))
				.doesNotContain(mariaOrderId);
	}

	@Test
	@DisplayName("Should combine the status filter with the search term")
	void combinesStatusAndSearch() {
		assertThat(idsMatching(new OrderSearchFilter(OrderStatus.SHIPPED, "maria.busca", null, null))).isEmpty();
		assertThat(idsMatching(new OrderSearchFilter(OrderStatus.PENDING_PAYMENT, "maria.busca", null, null)))
				.containsExactly(mariaOrderId);
	}

	/**
	 * O dia final entra inteiro. Com {@code createdAt < to.atStartOfDay()} um
	 * pedido feito às 14h do último dia do intervalo ficaria de fora — e o operador
	 * concluiria que ele não existe.
	 */
	@Test
	@DisplayName("Should include the whole of the last day in the range")
	void endDateIsInclusive() {
		LocalDate joaoDay = LocalDate.now().minusDays(1);

		// O pedido do João foi criado ontem no horário desta execução, e não à
		// meia-noite: com um fim exclusivo ele ficaria de fora deste intervalo de um
		// dia só. As asserções são por conteúdo, e não por igualdade da lista, porque
		// a base de desenvolvimento não está vazia.
		assertThat(idsMatching(new OrderSearchFilter(null, null, joaoDay, joaoDay))).contains(joaoOrderId)
				.doesNotContain(mariaOrderId);
	}

	@Test
	@DisplayName("Should narrow the listing by a date range")
	void filtersByRange() {
		List<Long> lastThreeDays = idsMatching(
				new OrderSearchFilter(null, null, LocalDate.now().minusDays(3), LocalDate.now()));

		assertThat(lastThreeDays).contains(joaoOrderId).doesNotContain(mariaOrderId);
	}

	/**
	 * Um intervalo invertido devolveria zero resultados sem erro nenhum, e o
	 * operador leria isso como "não há pedidos no período".
	 */
	@Test
	@DisplayName("Should refuse an inverted date range instead of returning nothing")
	void refusesInvertedRange() {
		OrderSearchFilter inverted = new OrderSearchFilter(null, null, LocalDate.now(), LocalDate.now().minusDays(5));

		assertThatThrownBy(() -> orderService.getAllOrders(inverted, PageRequest.of(0, 20)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("is after");
	}

	@Test
	@DisplayName("Should list everything when no filter is given")
	void emptyFilterListsEverything() {
		assertThat(idsMatching(new OrderSearchFilter(null, null, null, null))).contains(mariaOrderId, joaoOrderId);
	}

	// ---------------------------------------------------------------- helpers

	private List<Long> idsMatching(OrderSearchFilter filter) {
		return orderService.getAllOrders(filter, PageRequest.of(0, 50)).getContent().stream()
				.map(AdminOrderResponseDTO::id).toList();
	}

	private User persistUser(String email, String firstName, String lastName) {
		return userRepository.save(User.builder().email(email).firstName(firstName).lastName(lastName)
				.password("irrelevant").role(Role.CUSTOMER).emailVerified(true).build());
	}

	/**
	 * O createdAt é preenchido pelo auditing e não aceita valor no insert, então a
	 * data é ajustada por SQL depois — é o que permite testar a faixa sem esperar
	 * dias passarem.
	 */
	private Long persistOrder(User user, OrderStatus status, LocalDateTime createdAt) {
		Order order = orderRepository.saveAndFlush(Order.builder().user(user).status(status)
				.totalAmount(new BigDecimal("10.00")).shippingFee(BigDecimal.ZERO).expiresAt(createdAt.plusMinutes(30))
				.shippingAddress(ShippingAddress.builder().street("Rua A").number("1").neighborhood("Centro")
						.city("São Paulo").state("SP").postalCode("01001000").build())
				.build());

		entityManager.createNativeQuery("UPDATE orders SET created_at = :createdAt WHERE id = :id")
				.setParameter("createdAt", createdAt).setParameter("id", order.getId()).executeUpdate();

		return order.getId();
	}
}
