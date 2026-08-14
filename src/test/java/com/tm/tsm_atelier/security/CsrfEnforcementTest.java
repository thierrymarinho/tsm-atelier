package com.tm.tsm_atelier.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Servidor real, e nao MockMvc, pelo mesmo motivo de
 * AuthorizationStatusCodeTest: o que esta sendo testado e a cadeia de filtros
 * inteira, incluindo o cookie que o CsrfCookieFilter emite. Com .with(csrf())
 * do MockMvc o token e injetado por baixo do filtro, e o teste passaria mesmo
 * com a protecao desligada.
 *
 * A distincao importa para o front porque o CSRF ausente devolve 403, nao 401:
 * um interceptor que trate 403 como sessao expirada entra em laco de renovacao
 * e desloga um usuario cuja sessao estava perfeita.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("CSRF enforcement on writes")
class CsrfEnforcementTest {

	private static final String ADMIN_EMAIL = "csrf-probe-admin@example.com";

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

	private String accessToken;

	@BeforeEach
	void setUp() {
		cleanUp();
		User admin = userRepository.save(User.builder().firstName("Csrf").lastName("Probe").email(ADMIN_EMAIL)
				.password(passwordEncoder.encode("irrelevant-password")).role(Role.ADMIN).emailVerified(true).build());
		accessToken = jwtService.generateToken(admin);
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	@ParameterizedTest(name = "{0} {1} sem o header CSRF")
	@CsvSource({"POST,/api/v1/admin/products", "PUT,/api/v1/admin/products/1", "PATCH,/api/v1/admin/skus/1/stock",
			"DELETE,/api/v1/admin/products/1", "POST,/api/v1/admin/collections", "DELETE,/api/v1/admin/collections/1"})
	@DisplayName("Should answer 403 for a write without the CSRF header")
	void writeWithoutCsrfHeaderIsRefused(String method, String path) throws Exception {
		assertThat(send(method, path, null, null).statusCode()).isEqualTo(403);
	}

	/**
	 * A outra metade da prova. Sem isto, um 403 devolvido por qualquer outro motivo
	 * — rota inexistente, papel errado, filtro quebrado — faria o teste acima
	 * passar sem que a protecao de CSRF existisse.
	 */
	@Test
	@DisplayName("Should get past CSRF when the header carries the cookie value")
	void writeWithCsrfHeaderGetsPastTheFilter() throws Exception {
		String csrfToken = fetchCsrfToken();

		// Payload que desserializa e falha na validacao. O interesse aqui e so passar
		// do filtro: chegar na validacao de campos (422) prova que a requisicao foi
		// autorizada, e nao depende de criar um produto de verdade.
		String invalidButReadable = """
				{"name":"","price":-1,"category":"DRESSES","targetAudience":"WOMEN",
				 "active":true,"featured":false,"colors":[]}
				""";

		HttpResponse<String> response = send("POST", "/api/v1/admin/products", csrfToken, invalidButReadable);

		assertThat(response.statusCode()).as("com o header, a escrita passa do filtro de CSRF").isNotEqualTo(403);
		assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(422);
	}

	@Test
	@DisplayName("Should never require the CSRF header on reads")
	void readsDoNotRequireCsrf() throws Exception {
		assertThat(send("GET", "/api/v1/admin/products", null, null).statusCode()).isEqualTo(200);
	}

	// ---------------------------------------------------------------- helpers

	/**
	 * O cookie e emitido em qualquer resposta que passe pela cadeia de seguranca —
	 * inclusive num GET publico, que e como o front o obtem na inicializacao.
	 */
	private String fetchCsrfToken() throws Exception {
		HttpResponse<String> response = send("GET", "/api/v1/catalog/products", null, null);

		return response.headers().allValues("set-cookie").stream()
				.filter(cookie -> cookie.startsWith(SecurityConstants.CSRF_COOKIE + "="))
				.map(cookie -> cookie.substring((SecurityConstants.CSRF_COOKIE + "=").length()).split(";")[0])
				.findFirst().orElseThrow(
						() -> new AssertionError("nenhum cookie " + SecurityConstants.CSRF_COOKIE + " na resposta"));
	}

	private HttpResponse<String> send(String method, String path, String csrfToken, String body) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.method(method,
						body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
				.header("Content-Type", "application/json");

		List<String> cookies = new java.util.ArrayList<>();
		cookies.add(SecurityConstants.ACCESS_TOKEN_COOKIE + "=" + accessToken);

		if (csrfToken != null) {
			cookies.add(SecurityConstants.CSRF_COOKIE + "=" + csrfToken);
			request.header("X-XSRF-TOKEN", csrfToken);
		}

		request.header("Cookie", String.join("; ", cookies));

		return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	/** Ver a mesma limpeza em AuthorizationStatusCodeTest: User tem @SQLDelete. */
	private void cleanUp() {
		jdbcTemplate.update("DELETE FROM users WHERE email = ?", ADMIN_EMAIL);
	}
}
