package com.tm.tsm_atelier.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

/**
 * O primeiro admin vem de migration, e é a única conta capaz de abrir o painel:
 * o registro público cria sempre CUSTOMER e não existe rota para promover
 * ninguém.
 *
 * Isso torna a semente uma dependência silenciosa do encoder. Trocar
 * BCryptPasswordEncoder por outro algoritmo, ou mudar a força, não quebra
 * compilação e não quebra teste nenhum — quebra o login, em produção, da única
 * conta que consegue consertar qualquer coisa.
 */
@SpringBootTest
@TestPropertySource(properties = "app.scheduler.order-expiration.enabled=false")
@DisplayName("Admin seed")
class AdminSeedIntegrationTest {

	private static final String SEEDED_EMAIL = "admin@tsm-atelier.com";

	/** A senha documentada em ADMIN_FRONTEND_SPEC.md §1.4 e no application.yaml. */
	private static final String DEVELOPMENT_PASSWORD = "senha123";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * O hash que o Flyway vai substituir em V10, e não o que está gravado no banco.
	 * A distinção importa: trocar a senha no primeiro acesso é o comportamento
	 * esperado de quem receber esta conta, e um teste lendo a linha gravada
	 * passaria a falhar exatamente quando alguém fizesse a coisa certa.
	 */
	@Value("${spring.flyway.placeholders.admin_password_hash}")
	private String configuredHash;

	@Test
	@DisplayName("Should keep the development password readable by the configured encoder")
	void developmentHashMatchesTheConfiguredEncoder() {
		// Com ADMIN_PASSWORD_HASH definido, o hash resolvido é o do ambiente e não
		// corresponde à senha de desenvolvimento — o que é o objetivo da variável,
		// não uma falha.
		assumeTrue(System.getenv("ADMIN_PASSWORD_HASH") == null,
				"ADMIN_PASSWORD_HASH está definido; a senha em vigor não é a de desenvolvimento");

		assertThat(passwordEncoder.matches(DEVELOPMENT_PASSWORD, configuredHash))
				.as("o padrão de spring.flyway.placeholders.admin_password_hash não corresponde mais a '%s' "
						+ "sob o encoder de SecurityConfig — a única conta de administração ficaria sem login, "
						+ "e a credencial documentada na spec do front estaria errada", DEVELOPMENT_PASSWORD)
				.isTrue();
	}

	@Test
	@DisplayName("Should leave an admin able to reach the panel at all")
	void seededAdminCanReachThePanel() {
		User admin = userRepository.findByEmail(SEEDED_EMAIL)
				.orElseThrow(() -> new AssertionError("a semente de admin não foi aplicada: " + SEEDED_EMAIL));

		assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
		// A verificação depende de um link enviado a um endereço que não existe em
		// caixa nenhuma; sem isto a conta seria tão inacessível quanto a ausência dela.
		assertThat(admin.isEmailVerified()).isTrue();
	}
}
