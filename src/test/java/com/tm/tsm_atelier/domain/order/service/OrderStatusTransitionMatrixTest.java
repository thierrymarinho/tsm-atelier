package com.tm.tsm_atelier.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.tm.tsm_atelier.common.exception.custom.InvalidStatusTransitionException;
import com.tm.tsm_atelier.domain.admin.service.AuditService;
import com.tm.tsm_atelier.domain.cart.service.CartService;
import com.tm.tsm_atelier.domain.order.entity.Order;
import com.tm.tsm_atelier.domain.order.entity.OrderItem;
import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.entity.ShippingAddress;
import com.tm.tsm_atelier.domain.order.repository.OrderRepository;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.AddressRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * A matriz inteira, e nao alguns casos escolhidos a mao.
 *
 * Um mapa de transicoes e a classe de codigo onde a omissao nao se parece com
 * erro nenhum: esquecer um par proibido nao quebra compilacao, nao quebra teste
 * e so aparece quando alguem reabre um pedido cancelado em producao. Aqui os
 * trinta e seis pares sao gerados a partir do proprio enum — acrescentar um
 * status novo passa a cobri-lo automaticamente, e a duplicata do mapa abaixo
 * obriga a decisao a ser escrita duas vezes, o que e o ponto: o teste falha se
 * as duas versoes discordarem.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Order status transition matrix")
class OrderStatusTransitionMatrixTest {

	/**
	 * Repeticao deliberada do mapa de OrderService. Um teste que lesse a constante
	 * de producao concordaria com ela por construcao, inclusive quando ela
	 * estivesse errada.
	 */
	private static final Map<OrderStatus, Set<OrderStatus>> EXPECTED_TRANSITIONS = Map.of(OrderStatus.PENDING_PAYMENT,
			Set.of(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED), OrderStatus.PAYMENT_FAILED,
			Set.of(OrderStatus.PAID, OrderStatus.CANCELLED), OrderStatus.PAID,
			Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED), OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
			OrderStatus.DELIVERED, Set.of(), OrderStatus.CANCELLED, Set.of());

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private ProductSKURepository skuRepository;

	@Mock
	private AddressRepository addressRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private CartService cartService;

	@Mock
	private AuditService auditService;

	@InjectMocks
	private OrderService orderService;

	static Stream<Arguments> everyPair() {
		return Stream.of(OrderStatus.values()).flatMap(
				from -> Stream.of(OrderStatus.values()).filter(to -> to != from).map(to -> Arguments.of(from, to)));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("everyPair")
	@DisplayName("Should accept exactly the pairs in the flow and refuse every other one")
	void transitionMatrix(OrderStatus from, OrderStatus to) {
		Order order = anOrder(from);
		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

		// Cancelar devolve estoque, e so esse ramo toca o repositorio de SKU.
		lenient().when(skuRepository.findByIdWithPessimisticLock(99L))
				.thenReturn(Optional.of(order.getItems().getFirst().getSku()));

		if (EXPECTED_TRANSITIONS.get(from).contains(to)) {
			orderService.updateOrderStatus(1L, to);
			assertThat(order.getStatus()).isEqualTo(to);
		} else {
			assertThatThrownBy(() -> orderService.updateOrderStatus(1L, to))
					.isInstanceOf(InvalidStatusTransitionException.class);
			assertThat(order.getStatus()).as("uma transicao recusada nao pode ter efeito colateral").isEqualTo(from);
		}
	}

	/**
	 * O mesmo status de origem e destino nao e transicao: e um salvamento repetido,
	 * e devolver 400 ali faria um duplo clique parecer erro do operador.
	 */
	@ParameterizedTest(name = "{0} -> {0}")
	@MethodSource("everyStatus")
	@DisplayName("Should treat a no-op transition as success")
	void sameStatusIsANoOp(OrderStatus status) {
		Order order = anOrder(status);
		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

		orderService.updateOrderStatus(1L, status);

		assertThat(order.getStatus()).isEqualTo(status);
	}

	static Stream<OrderStatus> everyStatus() {
		return Stream.of(OrderStatus.values());
	}

	private Order anOrder(OrderStatus status) {
		ProductSKU sku = ProductSKU.builder().id(99L).skuCode("SKU-1").stockQuantity(10).build();

		User customer = User.builder().id(UUID.randomUUID()).firstName("Maria").lastName("Silva")
				.email("maria@example.com").build();

		return Order.builder().id(1L).user(customer).status(status).totalAmount(new BigDecimal("10.00"))
				.shippingFee(BigDecimal.ZERO)
				.shippingAddress(ShippingAddress.builder().street("Rua A").number("1").neighborhood("Centro")
						.city("São Paulo").state("SP").postalCode("01001000").build())
				.items(List.of(OrderItem.builder().id(1L).sku(sku).productName("Vestido").skuCode("SKU-1").size("M")
						.priceAtPurchase(new BigDecimal("10.00")).quantity(2).build()))
				.build();
	}
}
