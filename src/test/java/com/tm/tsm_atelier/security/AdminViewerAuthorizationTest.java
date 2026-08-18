package com.tm.tsm_atelier.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ROLE_ADMIN_VIEWER e o unico papel do sistema cuja permissao depende do metodo
 * HTTP, e nao so do caminho. Isso o torna fragil de um jeito especifico: a
 * regra "GET e leitura" e convencao, nao garantia, e um GET com efeito
 * colateral criado amanha entra no alcance deste papel sem erro, sem aviso e
 * sem diff na seguranca.
 *
 *
 * Por isso o teste fixa o conjunto pelos dois lados — o que o viewer alcanca e
 * o que ele nao alcanca —, e nao apenas o caminho feliz.
 *
 *
 * Vale contra servidor real pelo mesmo motivo registrado em
 * AuthorizationStatusCodeTest: o MockMvc nao executa o segundo despacho do
 * container, e foi ele que ja transformou 403 em 401 aqui uma vez.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("ROLE_ADMIN_VIEWER")
class AdminViewerAuthorizationTest {

	private static final String VIEWER_EMAIL = "viewer-probe@example.com";

	private static final String ADMIN_EMAIL = "viewer-probe-admin@example.com";

	private static final String CUSTOMER_EMAIL = "viewer-probe-customer@example.com";

	@LocalServerPort
	private int port;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final HttpClient client = HttpClient.newHttpClient();

	private String viewerToken;

	private String adminToken;

	private String customerToken;

	@BeforeEach
	void setUp() {
		cleanUp();
		viewerToken = jwtService.generateToken(save(VIEWER_EMAIL, Role.ADMIN_VIEWER));
		adminToken = jwtService.generateToken(save(ADMIN_EMAIL, Role.ADMIN));
		customerToken = jwtService.generateToken(save(CUSTOMER_EMAIL, Role.CUSTOMER));
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	@ParameterizedTest(name = "200 em GET {0}")
	@ValueSource(strings = {"/api/v1/admin/dashboard", "/api/v1/admin/products", "/api/v1/admin/collections",
			"/api/v1/admin/audit"})
	@DisplayName("Should let the viewer read the four panel areas it was granted")
	void viewerReadsTheGrantedAreas(String path) throws Exception {
		assertThat(send("GET", path, viewerToken, null, null).statusCode()).isEqualTo(200);
	}

	/**
	 * A rota que define o papel. Nao esta fora por causa do que a resposta mostra —
	 * esta fora porque o searchTerm casa por substring no e-mail e no nome do
	 * cliente, e isso responde "esta pessoa comprou aqui" a quem souber perguntar.
	 */
	@ParameterizedTest(name = "403 em GET {0}")
	@ValueSource(strings = {"/api/v1/admin/orders", "/api/v1/admin/orders/1",
			"/api/v1/admin/orders?searchTerm=maria@example.com"})
	@DisplayName("Should keep orders out of reach, including through the search filter")
	void viewerCannotReadOrders(String path) throws Exception {
		assertThat(send("GET", path, viewerToken, null, null).statusCode()).isEqualTo(403);
	}

	/**
	 * O CSRF vai preenchido de proposito. Sem ele a escrita seria recusada com 403
	 * pelo CsrfFilter, e o teste ficaria verde mesmo que alguem removesse a
	 * exigencia de papel — provando o filtro errado.
	 */
	@ParameterizedTest(name = "403 em {0} {1}")
	@CsvSource({"POST,/api/v1/admin/products", "PUT,/api/v1/admin/products/1", "DELETE,/api/v1/admin/products/1",
			"POST,/api/v1/admin/products/1/restore", "POST,/api/v1/admin/collections",
			"PUT,/api/v1/admin/collections/1", "DELETE,/api/v1/admin/collections/1",
			"POST,/api/v1/admin/collections/1/restore", "PATCH,/api/v1/admin/skus/1/stock",
			"POST,/api/v1/admin/uploads", "PATCH,/api/v1/admin/orders/1/status"})
	@DisplayName("Should refuse every write, even with a valid CSRF token")
	void viewerCannotWrite(String method, String path) throws Exception {
		assertThat(send(method, path, viewerToken, fetchCsrfToken(), "{}").statusCode()).isEqualTo(403);
	}

	@ParameterizedTest(name = "admin segue com 200 em GET {0}")
	@ValueSource(strings = {"/api/v1/admin/dashboard", "/api/v1/admin/products", "/api/v1/admin/collections",
			"/api/v1/admin/audit", "/api/v1/admin/orders"})
	@DisplayName("Should not narrow what ROLE_ADMIN could already reach")
	void adminKeepsFullReadAccess(String path) throws Exception {
		assertThat(send("GET", path, adminToken, null, null).statusCode()).isEqualTo(200);
	}

	/**
	 * A regra nova casa por metodo, e uma regra por metodo colocada na ordem errada
	 * passaria a responder tambem pelas escritas. Aqui o admin precisa atravessar a
	 * autorizacao: 400 ou 404 servem, 403 nao.
	 */
	@Test
	@DisplayName("Should still let ROLE_ADMIN through on writes to the same paths")
	void adminKeepsWriteAccess() throws Exception {
		assertThat(send("POST", "/api/v1/admin/products", adminToken, fetchCsrfToken(), "{}").statusCode())
				.isNotEqualTo(403);
	}

	@ParameterizedTest(name = "403 para CUSTOMER em GET {0}")
	@ValueSource(strings = {"/api/v1/admin/dashboard", "/api/v1/admin/products", "/api/v1/admin/collections",
			"/api/v1/admin/audit"})
	@DisplayName("Should not open the panel to an ordinary customer")
	void customerStaysOut(String path) throws Exception {
		assertThat(send("GET", path, customerToken, null, null).statusCode()).isEqualTo(403);
	}

	@Test
	@DisplayName("Should answer 401, not 403, when the viewer routes are hit without a session")
	void anonymousGetsUnauthorized() throws Exception {
		assertThat(send("GET", "/api/v1/admin/dashboard", null, null, null).statusCode()).isEqualTo(401);
	}

	// ---------------------------------------------------------------- helpers

	private User save(String email, Role role) {
		return userRepository.save(User.builder().firstName("Viewer").lastName("Probe").email(email)
				.password(passwordEncoder.encode("irrelevant-password")).role(role).emailVerified(true).build());
	}

	private String fetchCsrfToken() throws Exception {
		return send("GET", "/api/v1/catalog/products", null, null, null).headers().allValues("set-cookie").stream()
				.filter(cookie -> cookie.startsWith(SecurityConstants.CSRF_COOKIE + "="))
				.map(cookie -> cookie.substring((SecurityConstants.CSRF_COOKIE + "=").length()).split(";")[0])
				.findFirst().orElseThrow(
						() -> new AssertionError("nenhum cookie " + SecurityConstants.CSRF_COOKIE + " na resposta"));
	}

	private HttpResponse<String> send(String method, String path, String accessToken, String csrfToken, String body)
			throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.method(method,
						body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
				.header("Content-Type", "application/json");

		List<String> cookies = new ArrayList<>();

		if (accessToken != null) {
			cookies.add(SecurityConstants.ACCESS_TOKEN_COOKIE + "=" + accessToken);
		}

		if (csrfToken != null) {
			cookies.add(SecurityConstants.CSRF_COOKIE + "=" + csrfToken);
			request.header("X-XSRF-TOKEN", csrfToken);
		}

		if (!cookies.isEmpty()) {
			request.header("Cookie", String.join("; ", cookies));
		}

		return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	private void cleanUp() {
		jdbcTemplate.update("DELETE FROM users WHERE email IN (?, ?, ?)", VIEWER_EMAIL, ADMIN_EMAIL, CUSTOMER_EMAIL);
	}
}
