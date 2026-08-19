package com.tm.tsm_atelier.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tm.tsm_atelier.common.exception.custom.OutOfStockException;
import com.tm.tsm_atelier.domain.admin.service.AuditService;
import com.tm.tsm_atelier.domain.cart.service.CartService;
import com.tm.tsm_atelier.domain.order.dto.CheckoutItemDTO;
import com.tm.tsm_atelier.domain.order.dto.CheckoutRequestDTO;
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
 * É no checkout que a falha de estoque custa caro: o cliente já decidiu
 * comprar, e a tela onde ele lê o erro não é a mesma em que ele conserta.
 * Antes, para saber de qual item a frase falava, o front extraía o código do
 * SKU de dentro do texto em inglês com expressão regular — um contrato de dados
 * disfarçado de prosa, que morreria em silêncio na primeira reescrita da frase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService.createPendingOrder() sem estoque")
class OrderCheckoutOutOfStockTest {

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

	private static final Long SKU_ID = 4L;

	@Test
	@DisplayName("Estoque insuficiente identifica o SKU sem depender do texto do detail")
	void shouldIdentifyTheSkuOnInsufficientStock() {
		User user = givenCheckoutFor(aSku(0, true));

		assertThatThrownBy(() -> orderService.createPendingOrder(user, checkoutOf(2)))
				.isInstanceOfSatisfying(OutOfStockException.class, ex -> {
					assertThat(ex.getSkuId()).isEqualTo(SKU_ID);
					assertThat(ex.getReason()).isEqualTo(OutOfStockException.Reason.INSUFFICIENT_STOCK);
					assertThat(ex.getAvailableQuantity()).isZero();

					// O detail segue igual, em ingles e com o codigo interno: e'
					// texto de log, e o front nao precisa mais garimpa-lo.
					assertThat(ex.getMessage()).isEqualTo("Out of stock for SKU: TSM-000014. Available: 0");
				});
	}

	@Test
	@DisplayName("Produto desativado no meio do caminho reporta PRODUCT_UNAVAILABLE")
	void shouldReportAnInactiveProduct() {
		User user = givenCheckoutFor(aSku(30, false));

		assertThatThrownBy(() -> orderService.createPendingOrder(user, checkoutOf(1)))
				.isInstanceOfSatisfying(OutOfStockException.class, ex -> {
					assertThat(ex.getSkuId()).isEqualTo(SKU_ID);
					assertThat(ex.getReason()).isEqualTo(OutOfStockException.Reason.PRODUCT_UNAVAILABLE);
				});
	}

	private User givenCheckoutFor(ProductSKU sku) {
		User user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();

		Address address = Address.builder().id(10L).user(user).street("Street").number("1").neighborhood("Center")
				.city("City").state(State.SP).postalCode("12345678").build();

		when(addressRepository.findById(10L)).thenReturn(Optional.of(address));
		when(skuRepository.findByIdWithPessimisticLock(SKU_ID)).thenReturn(Optional.of(sku));

		return user;
	}

	private CheckoutRequestDTO checkoutOf(int quantity) {
		return new CheckoutRequestDTO(10L, List.of(new CheckoutItemDTO(SKU_ID, quantity)));
	}

	private ProductSKU aSku(int stockQuantity, boolean active) {
		Product product = Product.builder().id(1L).name("Vestido de Seda").slug("vestido-de-seda")
				.price(new BigDecimal("200.00")).active(active).build();

		ProductColor color = ProductColor.builder().id(3L).colorName("Preto").product(product).build();

		return ProductSKU.builder().id(SKU_ID).skuCode("TSM-000014").size(ProductSize.M).stockQuantity(stockQuantity)
				.productColor(color).build();
	}
}
