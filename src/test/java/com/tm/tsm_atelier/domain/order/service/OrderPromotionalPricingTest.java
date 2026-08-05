package com.tm.tsm_atelier.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tm.tsm_atelier.domain.cart.service.CartService;
import com.tm.tsm_atelier.domain.order.dto.CheckoutItemDTO;
import com.tm.tsm_atelier.domain.order.dto.CheckoutRequestDTO;
import com.tm.tsm_atelier.domain.order.entity.Order;
import com.tm.tsm_atelier.domain.order.entity.OrderItem;
import com.tm.tsm_atelier.domain.order.repository.OrderRepository;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.entity.ProductColor;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import com.tm.tsm_atelier.domain.user.entity.Address;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.enums.State;
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

/**
 * O preço era lido direto da entidade em quatro pontos independentes. O risco
 * de introduzir promoção não era a promoção não funcionar — era o catálogo
 * anunciar um valor e o checkout cobrar outro, sem nada acusando. Estes testes
 * fixam o lado que custa dinheiro.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService.createPendingOrder() com preço promocional")
class OrderPromotionalPricingTest {

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

	@InjectMocks
	private OrderService orderService;

	private static final BigDecimal LIST_PRICE = new BigDecimal("200.00");
	private static final BigDecimal PROMO_PRICE = new BigDecimal("150.00");

	@Test
	@DisplayName("Should charge the promotional price and freeze both prices on the order item")
	void shouldChargeThePromotionalPrice() {
		User user = user();
		ProductSKU sku = skuWith(PROMO_PRICE);

		stubCheckout(user, sku);

		Order order = orderService.createPendingOrder(user, checkoutOf(2));

		OrderItem item = order.getItems().get(0);
		assertThat(item.getPriceAtPurchase()).isEqualByComparingTo(PROMO_PRICE);
		assertThat(item.getListPriceAtPurchase()).isEqualByComparingTo(LIST_PRICE);

		// 2 x 150,00 — nao 2 x 200,00
		assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
	}

	@Test
	@DisplayName("Should charge the regular price when the product has no promotion")
	void shouldChargeTheRegularPriceWithoutPromotion() {
		User user = user();
		ProductSKU sku = skuWith(null);

		stubCheckout(user, sku);

		Order order = orderService.createPendingOrder(user, checkoutOf(1));

		OrderItem item = order.getItems().get(0);
		assertThat(item.getPriceAtPurchase()).isEqualByComparingTo(LIST_PRICE);

		// Sem promocao os dois valores coincidem, e o historico continua coerente.
		assertThat(item.getListPriceAtPurchase()).isEqualByComparingTo(LIST_PRICE);
		assertThat(order.getTotalAmount()).isEqualByComparingTo(LIST_PRICE);
	}

	private void stubCheckout(User user, ProductSKU sku) {
		Address address = Address.builder().id(10L).user(user).street("Street").number("1").neighborhood("Center")
				.city("City").state(State.SP).postalCode("12345678").build();

		when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
		when(skuRepository.findByIdWithPessimisticLock(4L)).thenReturn(Optional.of(sku));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private CheckoutRequestDTO checkoutOf(int quantity) {
		return new CheckoutRequestDTO(10L, List.of(new CheckoutItemDTO(4L, quantity)));
	}

	private User user() {
		return User.builder().id(UUID.randomUUID()).email("user@example.com").build();
	}

	private ProductSKU skuWith(BigDecimal promotionalPrice) {
		Product product = Product.builder().id(1L).name("Camisa").slug("camisa").price(LIST_PRICE)
				.promotionalPrice(promotionalPrice).active(true).build();

		ProductColor color = ProductColor.builder().id(3L).colorName("Preto").coverImageUrl("cover.jpg")
				.product(product).build();

		return ProductSKU.builder().id(4L).skuCode("SKU-1").size(ProductSize.M).stockQuantity(10).productColor(color)
				.build();
	}
}
