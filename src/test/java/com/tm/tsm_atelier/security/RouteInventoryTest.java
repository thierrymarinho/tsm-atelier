package com.tm.tsm_atelier.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * A autorizacao do admin e inteiramente <strong>posicional</strong>: uma unica
 * regra no SecurityConfig exige ROLE_ADMIN de tudo sob {@code /api/v1/admin/},
 * e nao ha {@code @PreAuthorize} em metodo nenhum. Isso funciona bem enquanto
 * for respeitado e falha em silencio quando nao for — um controller de admin
 * criado fora do prefixo fica acessivel a qualquer usuario logado, sem erro,
 * sem aviso e sem teste vermelho.
 *
 * <p>
 * Por isso este inventario. Ele nao julga se a rota esta no lugar certo, porque
 * o codigo nao tem como saber a intencao; ele obriga a decisao a aparecer no
 * diff. Rota nova quebra o teste, e quem atualizar a lista precisa olhar para o
 * prefixo enquanto o faz.
 */
@SpringBootTest
@DisplayName("Route inventory")
class RouteInventoryTest {

	/** Tudo aqui exige ROLE_ADMIN, pela regra de prefixo. */
	private static final Set<String> ADMIN_ROUTES = new TreeSet<>(List.of("/api/v1/admin/audit",
			"/api/v1/admin/collections", "/api/v1/admin/collections/{id}", "/api/v1/admin/collections/{id}/restore",
			"/api/v1/admin/dashboard", "/api/v1/admin/orders", "/api/v1/admin/orders/{id}",
			"/api/v1/admin/orders/{id}/status", "/api/v1/admin/products", "/api/v1/admin/products/{id}",
			"/api/v1/admin/products/{id}/restore", "/api/v1/admin/skus/{id}/stock", "/api/v1/admin/uploads"));

	/**
	 * Tudo aqui e publico ou exige apenas sessao. Uma rota de admin que aparecesse
	 * nesta lista estaria exposta — e e exatamente isso que o teste torna visivel.
	 */
	private static final Set<String> NON_ADMIN_ROUTES = new TreeSet<>(List.of("/api/v1/addresses",
			"/api/v1/addresses/{id}", "/api/v1/addresses/{id}/default", "/api/v1/auth/login", "/api/v1/auth/logout",
			"/api/v1/auth/me", "/api/v1/auth/refresh", "/api/v1/auth/register", "/api/v1/auth/resend-verification",
			"/api/v1/auth/verify-email", "/api/v1/cart", "/api/v1/cart/items", "/api/v1/cart/items/{itemId}",
			"/api/v1/cart/sync", "/api/v1/catalog/collections", "/api/v1/catalog/collections/{id}",
			"/api/v1/catalog/collections/slug/{slug}", "/api/v1/catalog/products", "/api/v1/catalog/products/{id}",
			"/api/v1/catalog/products/care-instructions", "/api/v1/catalog/products/categories",
			"/api/v1/catalog/products/materials", "/api/v1/catalog/products/slug/{slug}", "/api/v1/orders/checkout",
			"/api/v1/orders/my-orders", "/api/v1/orders/{id}", "/api/v1/webhooks/stripe"));

	@Autowired
	private RequestMappingHandlerMapping handlerMapping;

	@Test
	@DisplayName("Every admin route sits under the prefix the SecurityConfig protects")
	void adminRoutesAreUnderTheProtectedPrefix() {
		assertThat(mappedRoutes().stream().filter(route -> route.startsWith("/api/v1/admin/")).toList())
				.containsExactlyInAnyOrderElementsOf(ADMIN_ROUTES);
	}

	/**
	 * O lado que realmente pega o erro. Se um controller de admin nascer em
	 * {@code /api/v1/reports}, ele nao vai aparecer na lista de admin — vai
	 * aparecer aqui, como rota nova exigindo apenas sessao, e a falha diz
	 * exatamente isso.
	 */
	@Test
	@DisplayName("No route appears outside the prefix without the decision being made explicitly")
	void nonAdminRoutesAreAccountedFor() {
		assertThat(mappedRoutes().stream().filter(route -> !route.startsWith("/api/v1/admin/")).toList())
				.as("rota nova fora de /api/v1/admin — se ela e de administracao, esta acessivel a qualquer "
						+ "usuario logado; se nao e, acrescente-a a NON_ADMIN_ROUTES")
				.containsExactlyInAnyOrderElementsOf(NON_ADMIN_ROUTES);
	}

	/**
	 * Rotas da propria aplicacao, sem os endpoints do container e do actuator. O
	 * {@code distinct} agrupa os varios metodos HTTP de um mesmo caminho: o que
	 * interessa aqui e o path, que e o que a regra de seguranca enxerga.
	 */
	private List<String> mappedRoutes() {
		return handlerMapping.getHandlerMethods().keySet().stream().map(RequestMappingInfo::getPathPatternsCondition)
				.filter(java.util.Objects::nonNull)
				.flatMap(condition -> condition.getPatterns().stream().map(Object::toString))
				.filter(path -> path.startsWith("/api/")).distinct().toList();
	}
}
