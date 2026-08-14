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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

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
		customer = userRepository.save(User.builder().firstName("Status").lastName("Probe").email(CUSTOMER_EMAIL)
				.password(passwordEncoder.encode("irrelevant-password")).role(Role.CUSTOMER).emailVerified(true)
				.build());
	}

	@AfterEach
	void tearDown() {
		cleanUp();
	}

	@ParameterizedTest(name = "403 em {0}")
	@ValueSource(strings = {"/api/v1/admin/products", "/api/v1/admin/collections", "/api/v1/admin/orders",
			"/api/v1/admin/dashboard", "/api/v1/admin/skus/1/stock", "/api/v1/admin/uploads"})
	@DisplayName("Should answer 403 when an authenticated CUSTOMER hits any admin route group")
	void shouldAnswer403ForAuthenticatedCustomer(String path) throws Exception {
		assertThat(get(path, jwtService.generateToken(customer)).statusCode()).isEqualTo(403);
	}

	@ParameterizedTest(name = "401 em {0}")
	@ValueSource(strings = {"/api/v1/admin/products", "/api/v1/admin/collections", "/api/v1/admin/orders",
			"/api/v1/admin/dashboard", "/api/v1/admin/skus/1/stock", "/api/v1/admin/uploads"})
	@DisplayName("Should answer 401 when there is no session at all")
	void shouldAnswer401WithoutSession(String path) throws Exception {
		assertThat(get(path, null).statusCode()).isEqualTo(401);
	}

	@Test
	@DisplayName("Should not leak a 405 in place of the authorization answer")
	void authorizationDecidesBeforeMethodMatching() throws Exception {
		assertThat(get("/api/v1/admin/uploads", jwtService.generateToken(customer)).statusCode()).isNotEqualTo(405);
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

	private void cleanUp() {
		jdbcTemplate.update("DELETE FROM users WHERE email = ?", CUSTOMER_EMAIL);
	}
}
