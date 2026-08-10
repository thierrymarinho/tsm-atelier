package com.tm.tsm_atelier.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * Roda num servidor de verdade, e nao em MockMvc, de proposito. O bug que este
 * teste protege so existe no despacho de erro do container: o
 * AccessDeniedHandler chama response.sendError(403), o Tomcat despacha para
 * /error, e esse segundo despacho passa pela cadeia de seguranca outra vez — ja
 * sem SecurityContext. Enquanto /error nao era rota publica, ele caia em
 * anyRequest().authenticated() e o entry point sobrescrevia o 403 com 401.
 *
 * <p>
 * O MockMvc nao executa esse segundo despacho. A primeira versao deste teste
 * foi escrita com ele e passava com e sem a correcao — ou seja, nao provava
 * nada. Por isso aqui e servidor real.
 *
 * <p>
 * A diferenca importa para o front: 401 significa "sua sessao acabou", e um SPA
 * que renova o token ao ver 401 acaba deslogando um usuario cuja sessao estava
 * perfeita, so porque ele clicou onde nao tinha permissao.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("401 vs 403 through a real servlet container")
class AuthorizationStatusCodeTest {

	private static final String CUSTOMER_EMAIL = "status-code-probe@example.com";

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

	private User customer;

	@BeforeEach
	void setUp() {
		cleanUp();
		// Sem id: a entidade usa GenerationType.UUID, e um id preenchido faz o
		// Spring Data tratar o save como update de uma linha que nao existe.
		customer = userRepository.save(User.builder().firstName("Status").lastName("Probe").email(CUSTOMER_EMAIL)
				.password(passwordEncoder.encode("irrelevant-password")).role(Role.CUSTOMER).emailVerified(true)
				.build());
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	@Test
	@DisplayName("Should answer 403 when an authenticated CUSTOMER hits an admin route")
	void shouldAnswer403ForAuthenticatedCustomer() throws Exception {
		assertThat(get("/api/v1/admin/products", jwtService.generateToken(customer)).statusCode()).isEqualTo(403);
	}

	@Test
	@DisplayName("Should answer 401 when there is no session at all")
	void shouldAnswer401WithoutSession() throws Exception {
		assertThat(get("/api/v1/admin/products", null).statusCode()).isEqualTo(401);
	}

	@Test
	@DisplayName("Should keep the session usable on routes the CUSTOMER is allowed to reach")
	void shouldStillServeAllowedRoutes() throws Exception {
		HttpResponse<String> response = get("/api/v1/auth/me", jwtService.generateToken(customer));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains(CUSTOMER_EMAIL);
	}

	private HttpResponse<String> get(String path, String accessToken) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();

		if (accessToken != null) {
			request.header("Cookie", SecurityConstants.ACCESS_TOKEN_COOKIE + "=" + accessToken);
		}

		return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	/**
	 * DELETE direto, e nao userRepository.delete(): User tem @SQLDelete que
	 * anonimiza em vez de apagar (deleted_at preenchido, e-mail trocado por
	 * id@deleted.local). Pelo repositorio, cada execucao deixava uma linha
	 * permanente no banco — e como o e-mail muda na anonimizacao, a execucao
	 * seguinte nao a encontrava e criava outra.
	 */
	private void cleanUp() {
		jdbcTemplate.update("DELETE FROM users WHERE email = ?", CUSTOMER_EMAIL);
	}
}
