package com.tm.tsm_atelier.domain.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tm.tsm_atelier.common.exception.custom.OutOfStockException;
import com.tm.tsm_atelier.domain.cart.dto.CartItemRequestDTO;
import com.tm.tsm_atelier.domain.cart.entity.Cart;
import com.tm.tsm_atelier.domain.cart.entity.CartItem;
import com.tm.tsm_atelier.domain.cart.mapper.CartMapper;
import com.tm.tsm_atelier.domain.cart.repository.CartItemRepository;
import com.tm.tsm_atelier.domain.cart.repository.CartRepository;
import com.tm.tsm_atelier.domain.product.entity.Product;
import com.tm.tsm_atelier.domain.product.entity.ProductColor;
import com.tm.tsm_atelier.domain.product.entity.ProductSKU;
import com.tm.tsm_atelier.domain.product.enums.ProductSize;
import com.tm.tsm_atelier.domain.product.repository.ProductSKURepository;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * As três causas de 409 no carrinho chegavam ao cliente idênticas, e o front
 * separava a primeira pela aritmética — {@code availableQuantity >= 10} só
 * podia ser o teto por pedido. A inferência era correta e frágil: bastava mudar
 * o teto ou a ordem das checagens para o cliente passar a ler a mensagem
 * errada, sem nada acusando. O que estes testes fixam é o motivo explícito e a
 * identidade do item, que é o que o front precisa para nomear produto, cor e
 * tamanho na frase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartService — o 409 de estoque diz qual item e por quê")
class CartServiceOutOfStockTest {

	@Mock
	private CartRepository cartRepository;

	@Mock
	private CartItemRepository cartItemRepository;

	@Mock
	private ProductSKURepository skuRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CartMapper cartMapper;

	@InjectMocks
	private CartService cartService;

	private static final UUID USER_ID = UUID.randomUUID();
	private static final Long SKU_ID = 14L;

	@Test
	@DisplayName("Passar do teto de 10 unidades reporta MAX_UNITS_PER_ITEM e o próprio teto")
	void shouldReportTheCapAsItsOwnReason() {
		ProductSKU sku = aSku(100, true);
		givenCartWith(item(sku, 9));
		when(skuRepository.findById(SKU_ID)).thenReturn(Optional.of(sku));

		assertThatThrownBy(() -> cartService.addItem(USER_ID, new CartItemRequestDTO(SKU_ID, 2)))
				.isInstanceOfSatisfying(OutOfStockException.class, ex -> {
					assertThat(ex.getReason()).isEqualTo(OutOfStockException.Reason.MAX_UNITS_PER_ITEM);
					assertThat(ex.getSkuId()).isEqualTo(SKU_ID);

					// O teto viaja como dado; hoje o front o mantem copiado.
					assertThat(ex.getMaxUnitsPerItem()).isEqualTo(10);

					// availableQuantity continua sendo o teto, e nao o estoque:
					// e o campo que o front ja le, e mexer nele quebraria quem
					// ainda nao sabe do reason.
					assertThat(ex.getAvailableQuantity()).isEqualTo(10);
				});
	}

	@Test
	@DisplayName("Estoque insuficiente reporta INSUFFICIENT_STOCK com o SKU pedido")
	void shouldReportInsufficientStockOnAdd() {
		ProductSKU sku = aSku(1, true);
		givenEmptyCart();
		when(skuRepository.findById(SKU_ID)).thenReturn(Optional.of(sku));

		assertThatThrownBy(() -> cartService.addItem(USER_ID, new CartItemRequestDTO(SKU_ID, 3)))
				.isInstanceOfSatisfying(OutOfStockException.class, ex -> {
					assertThat(ex.getReason()).isEqualTo(OutOfStockException.Reason.INSUFFICIENT_STOCK);
					assertThat(ex.getSkuId()).isEqualTo(SKU_ID);
					assertThat(ex.getAvailableQuantity()).isEqualTo(1);
					assertThat(ex.getMaxUnitsPerItem()).isNull();
				});
	}

	@Test
	@DisplayName("Produto desativado reporta PRODUCT_UNAVAILABLE, e não estoque zerado")
	void shouldDistinguishAnInactiveProductFromEmptyStock() {
		ProductSKU sku = aSku(50, false);
		givenEmptyCart();
		when(skuRepository.findById(SKU_ID)).thenReturn(Optional.of(sku));

		assertThatThrownBy(() -> cartService.addItem(USER_ID, new CartItemRequestDTO(SKU_ID, 1)))
				.isInstanceOfSatisfying(OutOfStockException.class, ex -> {
					assertThat(ex.getReason()).isEqualTo(OutOfStockException.Reason.PRODUCT_UNAVAILABLE);
					assertThat(ex.getSkuId()).isEqualTo(SKU_ID);

					// O item saiu de linha com 50 pecas no estoque: sem o reason,
					// este 409 e' identico ao de estoque zerado.
					assertThat(ex.getAvailableQuantity()).isZero();
				});
	}

	@Test
	@DisplayName("Alterar a quantidade acima do estoque também identifica o SKU")
	void shouldReportInsufficientStockOnUpdate() {
		ProductSKU sku = aSku(2, true);
		givenCartWith(item(sku, 1));

		assertThatThrownBy(() -> cartService.updateItemQuantity(USER_ID, 7L, 5))
				.isInstanceOfSatisfying(OutOfStockException.class, ex -> {
					assertThat(ex.getReason()).isEqualTo(OutOfStockException.Reason.INSUFFICIENT_STOCK);
					assertThat(ex.getSkuId()).isEqualTo(SKU_ID);
					assertThat(ex.getAvailableQuantity()).isEqualTo(2);
				});
	}

	private void givenEmptyCart() {
		when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(new Cart()));
	}

	private void givenCartWith(CartItem existing) {
		Cart cart = new Cart();
		cart.addItem(existing);
		when(cartRepository.findByUserIdWithItems(USER_ID)).thenReturn(Optional.of(cart));
	}

	private CartItem item(ProductSKU sku, int quantity) {
		CartItem item = new CartItem();
		item.setId(7L);
		item.setSku(sku);
		item.setQuantity(quantity);
		return item;
	}

	private ProductSKU aSku(int stockQuantity, boolean active) {
		Product product = Product.builder().id(1L).name("Vestido de Seda").slug("vestido-de-seda")
				.price(new BigDecimal("200.00")).active(active).build();

		ProductColor color = ProductColor.builder().id(3L).colorName("Preto").product(product).build();

		return ProductSKU.builder().id(SKU_ID).skuCode("TSM-000014").size(ProductSize.M).stockQuantity(stockQuantity)
				.productColor(color).build();
	}
}
