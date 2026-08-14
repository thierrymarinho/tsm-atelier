package com.tm.tsm_atelier.domain.cart.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tm.tsm_atelier.domain.cart.service.CartService;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import com.tm.tsm_atelier.security.AccessTokenDenylist;
import com.tm.tsm_atelier.security.JwtService;
import com.tm.tsm_atelier.security.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A lista de itens do sync so tem suas restricoes aplicadas por causa do @Valid
 * no campo. O @NotNull sozinho valida a lista, nao os itens dentro dela — e o
 * item invalido seguia ate o service.
 *
 * Estes testes existem porque a falha era silenciosa das duas pontas: nao havia
 * erro de compilacao, e a requisicao respondia 200 ou 500 conforme o dado. Cada
 * caso abaixo falha se o @Valid for removido.
 */
@WebMvcTest(CartController.class)
@Import(SecurityConfig.class)
@DisplayName("POST /api/v1/cart/sync validation")
class CartSyncValidationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CartService cartService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private AccessTokenDenylist accessTokenDenylist;

	private static final String SYNC_URL = "/api/v1/cart/sync";

	@Test
	@DisplayName("Should reject a null quantity inside the items list instead of failing at unboxing")
	void shouldRejectNullQuantityInsideTheList() throws Exception {
		// Sem o @Valid isto chegava ao service e estourava no unboxing de Integer
		// para int — 500 para uma requisicao que o cliente montou errado.
		sync("""
				{"items": [{"skuId": 1, "quantity": null}]}
				""").andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.fields['items[0].quantity']").value("Quantity is required"));

		verify(cartService, never()).syncCart(any(UUID.class), any());
	}

	@Test
	@DisplayName("Should reject a null SKU id inside the items list")
	void shouldRejectNullSkuIdInsideTheList() throws Exception {
		sync("""
				{"items": [{"skuId": null, "quantity": 2}]}
				""").andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.fields['items[0].skuId']").value("SKU ID is required"));

		verify(cartService, never()).syncCart(any(UUID.class), any());
	}

	@Test
	@DisplayName("Should reject a quantity below one instead of letting the database check constraint fail")
	void shouldRejectQuantityBelowOne() throws Exception {
		// Antes, o Math.min do service deixava passar e o CHECK (quantity > 0) do
		// banco quebrava, devolvendo 409 "A data conflict occurred".
		sync("""
				{"items": [{"skuId": 1, "quantity": -5}]}
				""").andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.fields['items[0].quantity']").value("Quantity must be at least 1"));

		verify(cartService, never()).syncCart(any(UUID.class), any());
	}

	@Test
	@DisplayName("Should reject a quantity above the per-item limit")
	void shouldRejectQuantityAboveTheLimit() throws Exception {
		sync("""
				{"items": [{"skuId": 1, "quantity": 9999}]}
				""").andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.fields['items[0].quantity']").value("Maximum 10 units per item"));

		verify(cartService, never()).syncCart(any(UUID.class), any());
	}

	@Test
	@DisplayName("Should accept a valid items list")
	void shouldAcceptAValidItemsList() throws Exception {
		sync("""
				{"items": [{"skuId": 1, "quantity": 2}]}
				""").andExpect(status().isOk());

		verify(cartService).syncCart(any(UUID.class), any());
	}

	private org.springframework.test.web.servlet.ResultActions sync(String body) throws Exception {
		return mockMvc.perform(post(SYNC_URL).with(user(userPrincipal())).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private com.tm.tsm_atelier.domain.user.entity.User userPrincipal() {
		return com.tm.tsm_atelier.domain.user.entity.User.builder().id(UUID.randomUUID()).email("user@example.com")
				.password("x").build();
	}
}
