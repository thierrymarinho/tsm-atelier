package com.tm.tsm_atelier.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tm.tsm_atelier.domain.admin.service.AuditService;
import com.tm.tsm_atelier.domain.cart.service.CartService;
import com.tm.tsm_atelier.domain.order.dto.AdminOrderResponseDTO;
import com.tm.tsm_atelier.domain.order.dto.OrderResponseDTO;
import com.tm.tsm_atelier.domain.order.dto.OrderSearchFilter;
import com.tm.tsm_atelier.domain.order.entity.Order;
import com.tm.tsm_atelier.domain.order.entity.OrderItem;
import com.tm.tsm_atelier.domain.order.entity.OrderStatus;
import com.tm.tsm_atelier.domain.order.entity.ShippingAddress;
import com.tm.tsm_atelier.domain.order.repository.OrderRepository;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.AddressRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("What the admin sees of an order")
class AdminOrderViewTest {

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

	private static final UUID CUSTOMER_ID = UUID.randomUUID();

	/**
	 * A listagem não trazia nome, e-mail nem id do comprador — o mais perto disso
	 * era o endereço de entrega —, então não havia como atender um cliente sem ir
	 * ao banco.
	 */
	@Test
	@DisplayName("Should identify the customer in the listing")
	void shouldIdentifyTheCustomer() {
		when(orderRepository.findAll(ArgumentMatchers.<Specification<Order>>any(), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(anOrder(OrderStatus.PENDING_PAYMENT))));

		AdminOrderResponseDTO listed = orderService
				.getAllOrders(new OrderSearchFilter(null, null, null, null), PageRequest.of(0, 20)).getContent()
				.getFirst();

		assertThat(listed.customerId()).isEqualTo(CUSTOMER_ID);
		assertThat(listed.customerName()).isEqualTo("Maria Silva");
		assertThat(listed.customerEmail()).isEqualTo("maria@example.com");
	}

	/**
	 * O clientSecret é a credencial que o navegador usa para confirmar o pagamento
	 * na Stripe. A listagem do admin carregava o de todos os pedidos pendentes de
	 * uma vez, e o painel não faz nada com ele.
	 *
	 * <p>
	 * O record de admin simplesmente não tem o campo — a garantia é estrutural, não
	 * uma checagem que alguém possa esquecer de repetir num mapeamento novo.
	 */
	@Test
	@DisplayName("Should not carry the payment client secret into the admin view")
	void shouldNotCarryTheClientSecret() {
		assertThat(AdminOrderResponseDTO.class.getRecordComponents())
				.extracting(java.lang.reflect.RecordComponent::getName).doesNotContain("clientSecret");
	}

	/**
	 * A rota do cliente virou exclusiva do dono, inclusive para o ADMIN. Antes ele
	 * passava por aqui e recebia a resposta do cliente com o clientSecret anulado
	 * por um parâmetro booleano — uma garantia que dependia de alguém lembrar de
	 * passar {@code false}.
	 */
	@Test
	@DisplayName("Should refuse an admin on the customer route instead of stripping fields at runtime")
	void shouldRefuseAdminOnTheCustomerRoute() {
		Order order = anOrder(OrderStatus.PENDING_PAYMENT);
		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

		User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();

		assertThatThrownBy(() -> orderService.getOrderDetails(1L, admin)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@DisplayName("Should give the admin its own detail, with the customer identified")
	void shouldGiveTheAdminItsOwnDetail() {
		Order order = anOrder(OrderStatus.PENDING_PAYMENT);
		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

		AdminOrderResponseDTO seenByAdmin = orderService.getAdminOrderDetails(1L);

		assertThat(seenByAdmin.customerEmail()).isEqualTo("maria@example.com");
		assertThat(seenByAdmin.customerId()).isEqualTo(CUSTOMER_ID);
		assertThat(seenByAdmin.items()).hasSize(1);
	}

	@Test
	@DisplayName("Should keep the client secret for the customer who owns the order")
	void shouldKeepClientSecretForTheOwner() {
		Order order = anOrder(OrderStatus.PENDING_PAYMENT);
		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

		OrderResponseDTO seenByOwner = orderService.getOrderDetails(1L, order.getUser());

		assertThat(seenByOwner.clientSecret()).isEqualTo("pi_123_secret_abc");
	}

	/**
	 * O @SQLRestriction do ProductSKU esconde SKUs retirados do catálogo, e a
	 * versão anterior usava ifPresent: as unidades sumiam da contabilidade sem
	 * exceção e sem log, e o cancelamento seguia como se tivesse devolvido tudo.
	 */
	@Test
	@DisplayName("Should not fail when the SKU of a cancelled order is no longer in the catalog")
	void shouldSurviveARemovedSku() {
		Order order = anOrder(OrderStatus.PAID);
		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
		when(skuRepository.findByIdWithPessimisticLock(99L)).thenReturn(Optional.empty());

		orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		verify(skuRepository).findByIdWithPessimisticLock(99L);
	}

	@Test
	@DisplayName("Should restore stock through the admin view as well")
	void shouldRestoreStock() {
		Order order = anOrder(OrderStatus.PAID);
		ProductSKU sku = order.getItems().getFirst().getSku();

		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
		when(skuRepository.findByIdWithPessimisticLock(99L)).thenReturn(Optional.of(sku));

		AdminOrderResponseDTO updated = orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

		assertThat(sku.getStockQuantity()).isEqualTo(12);
		assertThat(updated.customerEmail()).isEqualTo("maria@example.com");
		verify(eventPublisher, never()).publishEvent(any());
	}

	private Order anOrder(OrderStatus status) {
		ProductSKU sku = ProductSKU.builder().id(99L).skuCode("SKU-1").stockQuantity(10).build();

		User customer = User.builder().id(CUSTOMER_ID).firstName("Maria").lastName("Silva").email("maria@example.com")
				.role(Role.CUSTOMER).build();

		return Order.builder().id(1L).user(customer).status(status).totalAmount(new BigDecimal("10.00"))
				.shippingFee(BigDecimal.ZERO).paymentIntentId("pi_123").paymentClientSecret("pi_123_secret_abc")
				.shippingAddress(ShippingAddress.builder().street("Rua A").number("1").neighborhood("Centro")
						.city("São Paulo").state("SP").postalCode("01001000").build())
				.items(List.of(OrderItem.builder().id(1L).sku(sku).productName("Vestido").skuCode("SKU-1").size("M")
						.priceAtPurchase(new BigDecimal("10.00")).quantity(2).build()))
				.build();
	}
}
