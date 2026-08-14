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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * O servidor não pode destruir a credencial que ele mesmo exige.
 *
 * O defeito. Sob SessionCreationPolicy.STATELESS o Spring Security ainda
 * registra o SessionManagementFilter, e ele chamava CsrfAuthenticationStrategy
 * em toda requisição autenticada — que começa por saveToken(null, ...), ou
 * seja, um Set-Cookie de deleção do __Host-XSRF-TOKEN. O CsrfCookieFilter
 * existe para materializar o token novo, mas estava registrado quatro posições
 * antes do filtro que apagava, então a deleção vinha por último e vencia.
 *
 * Por que este teste, e não o óbvio. O sintoma relatado era "escritas alternam
 * entre funcionar e falhar", e a leitura natural seria afirmar que N escritas
 * seguidas passam. Só que esse teste passava mesmo com o defeito: o
 * CookieCsrfTokenRepository não tem estado — o cookie é o armazenamento —,
 * então apagar o cookie não invalida o valor, e um cliente que segure o token
 * em memória nunca vê 403. Quem quebrava era o cliente que relê o cookie e o
 * encontra vazio.
 *
 * A asserção que realmente pega o defeito é a ausência do Set-Cookie de
 * deleção. O caso das seis escritas fica assim mesmo, porque é o sintoma que
 * alguém vai procurar aqui quando isto reaparecer — mas com o comentário
 * dizendo que sozinho ele não prova nada.
 *
 * Servidor real, e não MockMvc, pelo mesmo motivo de CsrfEnforcementTest: o que
 * está sob teste é a ordem da cadeia de filtros, que o MockMvc não reproduz.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("CSRF token lifecycle")
class CsrfTokenLifecycleTest {

	private static final String ADMIN_EMAIL = "csrf-lifecycle-admin@example.com";

	/**
	 * Desserializa e falha na validação: chega ao controller, provando que passou
	 * do CsrfFilter.
	 */
	private static final String READABLE_BUT_INVALID = """
			{"name":"","price":-1,"category":"DRESSES","targetAudience":"WOMEN",
			 "active":true,"featured":false,"colors":[]}
			""";

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
		User admin = userRepository.save(User.builder().firstName("Csrf").lastName("Lifecycle").email(ADMIN_EMAIL)
				.password(passwordEncoder.encode("irrelevant-password")).role(Role.ADMIN).emailVerified(true).build());
		accessToken = jwtService.generateToken(admin);
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	/**
	 * O gatilho do defeito era "autenticada e carregando o cookie", sem relação com
	 * o método HTTP: um GET apagava igual a um PATCH.
	 */
	@Test
	@DisplayName("Should not delete the csrf cookie on an authenticated read")
	void authenticatedReadKeepsTheCookie() throws Exception {
		String token = fetchCsrfToken();

		HttpResponse<String> response = send("GET", "/api/v1/auth/me", token, true, null);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(deletionsIn(response)).as("um GET autenticado não pode apagar o token da escrita seguinte")
				.isEmpty();
	}

	@Test
	@DisplayName("Should not delete the csrf cookie on a write that got past the filter")
	void successfulWriteKeepsTheCookie() throws Exception {
		String token = fetchCsrfToken();

		HttpResponse<String> response = send("POST", "/api/v1/admin/products", token, true, READABLE_BUT_INVALID);

		assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(422);
		assertThat(deletionsIn(response)).isEmpty();
	}

	/**
	 * O sintoma que o relatório descreve, reproduzido como o cliente o vive: sem
	 * nenhuma leitura entre as escritas, e reenviando o que o servidor mandou por
	 * último.
	 *
	 * Sozinho ele não prova a correção — passa mesmo com o defeito, porque o
	 * repositório é sem estado. O que ele acrescenta é a segunda asserção: o token
	 * em vigor tem que continuar o mesmo do começo. Uma correção que apenas
	 * reemitisse o cookie depois de apagá-lo faria as seis escritas passarem e
	 * rotacionaria o token a cada resposta, o que é uma corrida esperando
	 * requisições concorrentes.
	 */
	@Test
	@DisplayName("Should carry one token through six consecutive writes")
	void consecutiveWritesShareOneStableToken() throws Exception {
		String token = fetchCsrfToken();

		for (int attempt = 1; attempt <= 6; attempt++) {
			HttpResponse<String> response = send("POST", "/api/v1/admin/products", token, true, READABLE_BUT_INVALID);

			assertThat(response.statusCode()).as("escrita %d de 6", attempt).isNotEqualTo(403);
			assertThat(deletionsIn(response)).as("escrita %d de 6 apagou o cookie", attempt).isEmpty();
			assertThat(issuedTokenIn(response)).as("escrita %d de 6 trocou o token em vigor", attempt).isNull();
		}
	}

	/**
	 * A proteção não pode ter ido embora junto com o defeito. O caso do header
	 * ausente está em CsrfEnforcementTest, sobre seis rotas; o que falta é o par
	 * desencontrado, que é o que um ataque de fato produz — o navegador manda o
	 * cookie da vítima, e o atacante não consegue ler o valor para montar o header.
	 */
	@Test
	@DisplayName("Should still refuse a write whose header does not match the cookie")
	void mismatchedTokenIsRefused() throws Exception {
		String token = fetchCsrfToken();

		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/admin/products"))
				.method("POST", HttpRequest.BodyPublishers.ofString(READABLE_BUT_INVALID))
				.header("Content-Type", "application/json")
				.header("Cookie", SecurityConstants.ACCESS_TOKEN_COOKIE + "=" + accessToken + "; "
						+ SecurityConstants.CSRF_COOKIE + "=" + token)
				.header("X-XSRF-TOKEN", "00000000-0000-0000-0000-000000000000").build();

		assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
	}

	/**
	 * A contrapartida da correção. Desligar o configurer de sessão tirou o
	 * SessionManagementFilter da cadeia, mas levou junto a declaração STATELESS: a
	 * ausência de sessão passou a ser consequência de nada criá-la, e não de uma
	 * política escrita.
	 *
	 * Isso é verdade hoje porque o JwtAuthenticationFilter escreve só no
	 * SecurityContextHolder e ninguém chama saveContext. É exatamente o tipo de
	 * premissa que um formLogin, um rememberMe ou um filtro novo derrubam sem aviso
	 * — e a primeira evidência seria um JSESSIONID aparecendo em produção.
	 */
	@Test
	@DisplayName("Should never create an http session")
	void noHttpSessionIsCreated() throws Exception {
		String token = fetchCsrfToken();

		List<HttpResponse<String>> responses = List.of(send("GET", "/api/v1/catalog/products", null, false, null),
				send("GET", "/api/v1/auth/me", token, true, null),
				send("POST", "/api/v1/admin/products", token, true, READABLE_BUT_INVALID));

		assertThat(responses).allSatisfy(response -> assertThat(response.headers().allValues("set-cookie"))
				.as("a ausência de sessão não é mais declarada; é consequência de nada criá-la")
				.noneMatch(cookie -> cookie.startsWith("JSESSIONID=")));
	}

	// ---------------------------------------------------------------- helpers

	/** O Set-Cookie de deleção: valor vazio e Expires em 1970. */
	private List<String> deletionsIn(HttpResponse<String> response) {
		return response.headers().allValues("set-cookie").stream()
				.filter(cookie -> cookie.startsWith(SecurityConstants.CSRF_COOKIE + "=;")).toList();
	}

	/**
	 * O token novo que a resposta emitiu, ou null se ela não mexeu no cookie.
	 */
	private String issuedTokenIn(HttpResponse<String> response) {
		return response.headers().allValues("set-cookie").stream()
				.filter(cookie -> cookie.startsWith(SecurityConstants.CSRF_COOKIE + "="))
				.map(cookie -> cookie.substring((SecurityConstants.CSRF_COOKIE + "=").length()).split(";")[0])
				.filter(value -> !value.isEmpty()).findFirst().orElse(null);
	}

	private String fetchCsrfToken() throws Exception {
		HttpResponse<String> response = send("GET", "/api/v1/catalog/products", null, false, null);

		return issuedTokenIn(response);
	}

	private HttpResponse<String> send(String method, String path, String csrfToken, boolean authenticated, String body)
			throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
				.method(method,
						body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
				.header("Content-Type", "application/json");

		List<String> cookies = new ArrayList<>();
		if (authenticated) {
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

	/** Ver a mesma limpeza em CsrfEnforcementTest: User tem @SQLDelete. */
	private void cleanUp() {
		jdbcTemplate.update("DELETE FROM users WHERE email = ?", ADMIN_EMAIL);
	}
}
