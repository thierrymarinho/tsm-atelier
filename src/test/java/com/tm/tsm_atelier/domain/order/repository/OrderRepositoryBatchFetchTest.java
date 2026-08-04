package com.tm.tsm_atelier.domain.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixa o ganho do default_batch_fetch_size: sem ele, montar o DTO de uma página
 * de pedidos disparava uma consulta por pedido só para carregar os itens. Fetch
 * join resolveria o N+1 também, mas obrigaria o Hibernate a paginar em memória,
 * então a checagem aqui é sobre o número de consultas, não sobre o plano usado.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class OrderRepositoryBatchFetchTest {

	private static final int ORDER_COUNT = 5;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	@DisplayName("Carrega os itens de uma página inteira de pedidos sem uma consulta por pedido")
	void loadsItemsForAWholePageWithoutOneQueryPerOrder() {
		UUID userId = seedUserWithOrders();
		entityManager.flush();
		entityManager.clear();

		Statistics statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
		statistics.clear();

		var page = orderRepository.findByUserId(userId, PageRequest.of(0, ORDER_COUNT));
		page.getContent().forEach(order -> assertThat(order.getItems()).hasSize(1));

		assertThat(page.getContent()).hasSize(ORDER_COUNT);
		// 1 consulta para os pedidos + 1 para os itens de todos eles.
		assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
	}

	private UUID seedUserWithOrders() {
		UUID userId = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO users (id, first_name, last_name, email, password, role, email_verified) "
						+ "VALUES (?, 'Batch', 'Fetch', ?, 'x', 'CUSTOMER', true)",
				userId, "batch-fetch-" + userId + "@example.com");

		for (int i = 0; i < ORDER_COUNT; i++) {
			Long orderId = jdbcTemplate.queryForObject("INSERT INTO orders (user_id, status, total_amount, "
					+ "shipping_fee, street, number, neighborhood, city, state, postal_code, expires_at) "
					+ "VALUES (?, 'PAID', 10.00, 0.00, 'Street', '1', 'Center', 'City', 'SP', '12345678', "
					+ "CURRENT_TIMESTAMP) RETURNING id", Long.class, userId);

			jdbcTemplate.update(
					"INSERT INTO order_items (order_id, product_name, sku_code, size, "
							+ "price_at_purchase, quantity) VALUES (?, 'Product', ?, 'M', 10.00, 1)",
					orderId, "BATCH-" + orderId);
		}

		return userId;
	}
}
