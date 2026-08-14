package com.tm.tsm_atelier.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService.updateOrderStatus()")
class OrderServiceStatusTest {

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

	@Test
	@DisplayName("Should reject a transition that does not exist in the order flow")
	void shouldRejectTransitionOutsideTheOrderFlow() {
		Order order = anOrder(OrderStatus.DELIVERED, 2);
		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PENDING_PAYMENT))
				.isInstanceOf(InvalidStatusTransitionException.class)
				.hasMessage("Cannot move an order from DELIVERED to PENDING_PAYMENT.");

		assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
	}

	@Test
	@DisplayName("Should restore stock when a paid order is cancelled")
	void shouldRestoreStockWhenCancellingAPaidOrder() {
		Order order = anOrder(OrderStatus.PAID, 2);
		ProductSKU sku = order.getItems().get(0).getSku();

		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));
		when(skuRepository.findByIdWithPessimisticLock(sku.getId())).thenReturn(Optional.of(sku));

		orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(sku.getStockQuantity()).isEqualTo(12);
	}

	@Test
	@DisplayName("Should advance the status without touching stock on a valid transition")
	void shouldAdvanceStatusWithoutTouchingStock() {
		Order order = anOrder(OrderStatus.PAID, 2);
		when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(order));

		orderService.updateOrderStatus(1L, OrderStatus.SHIPPED);

		assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
		verify(skuRepository, never()).findByIdWithPessimisticLock(any());
	}

	private Order anOrder(OrderStatus status, int quantity) {
		ProductSKU sku = ProductSKU.builder().id(99L).skuCode("SKU-1").stockQuantity(10).build();

		Order order = Order.builder().id(1L).user(User.builder().id(UUID.randomUUID()).build()).status(status)
				.totalAmount(new BigDecimal("10.00")).shippingFee(BigDecimal.ZERO)
				.shippingAddress(ShippingAddress.builder().street("Street").number("1").neighborhood("Center")
						.city("City").state("SP").postalCode("12345678").build())
				.items(List.of(OrderItem.builder().id(1L).sku(sku).productName("Product").skuCode("SKU-1").size("M")
						.priceAtPurchase(new BigDecimal("10.00")).quantity(quantity).build()))
				.build();

		return order;
	}
}
