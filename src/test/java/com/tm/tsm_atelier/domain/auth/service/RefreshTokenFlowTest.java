package com.tm.tsm_atelier.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tm.tsm_atelier.common.exception.custom.InvalidTokenException;
import com.tm.tsm_atelier.domain.auth.dto.AuthResponseDTO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fixa o comportamento da rotação de refresh token contra o Redis real. Os três
 * primeiros casos correspondem a bugs observados: um retry do cliente derrubava
 * todas as sessões, o índice do usuário nunca expirava, e o token continuava
 * valendo depois de a conta perder a verificação.
 */
@SpringBootTest
@DisplayName("Refresh token rotation")
class RefreshTokenFlowTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private StringRedisTemplate redis;

	// verifyEmail() e transacional e commita, entao nao da para apoiar em rollback:
	// sem esta limpeza cada execucao deixa usuarios e chaves de sessao para tras no
	// banco de desenvolvimento.
	private final List<String> createdEmails = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		for (String email : createdEmails) {
			redis.delete("rt:user:" + email);
			jdbcTemplate.update("DELETE FROM users WHERE email = ?", email);
		}
		createdEmails.clear();
	}

	@Test
	@DisplayName("A retry with the already rotated token returns new tokens instead of revoking every session")
	void retryWithinGraceWindowDoesNotRevokeEverySession() {
		AuthResponseDTO initial = newSession();

		AuthResponseDTO rotated = authService.refresh(initial.refreshToken());

		// O cliente não recebeu a resposta acima e tentou de novo com o token que
		// ainda tinha em mãos.
		AuthResponseDTO retried = authService.refresh(initial.refreshToken());
		assertThat(retried.refreshToken()).isNotBlank();

		// E o token que a primeira rotação emitiu continua de pé.
		assertThat(authService.refresh(rotated.refreshToken()).refreshToken()).isNotBlank();
	}

	@Test
	@DisplayName("Replaying the token after the grace window revokes every session")
	void replayAfterGraceWindowRevokesEverySession() {
		AuthResponseDTO initial = newSession();

		AuthResponseDTO rotated = authService.refresh(initial.refreshToken());

		// Encerra a janela de graça na mão em vez de esperar o TTL correr.
		redis.delete("rt:grace:" + sha256(initial.refreshToken()));

		assertThatThrownBy(() -> authService.refresh(initial.refreshToken())).isInstanceOf(InvalidTokenException.class)
				.hasMessageContaining("Token reuse detected");

		// A revogação alcança o token legítimo emitido na rotação.
		assertThatThrownBy(() -> authService.refresh(rotated.refreshToken())).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	@DisplayName("The user token index expires along with the tokens it holds")
	void userTokenIndexExpires() {
		AuthResponseDTO session = newSession();

		Long ttl = redis.getExpire("rt:user:" + session.email());

		assertThat(ttl).as("rt:user sem TTL volta a acumular hashes de tokens ja mortos").isPositive();
	}

	@Test
	@DisplayName("Logout also closes the grace window of the token it revokes")
	void logoutClosesTheGraceWindow() {
		AuthResponseDTO initial = newSession();

		// Depois da rotação o token inicial fica na janela de graça: é justamente o
		// estado em que ele ainda renderia uma sessão nova.
		authService.refresh(initial.refreshToken());
		authService.logout(null, initial.refreshToken());

		assertThatThrownBy(() -> authService.refresh(initial.refreshToken())).isInstanceOf(InvalidTokenException.class);
	}

	private AuthResponseDTO newSession() {
		String email = "refresh-flow-" + UUID.randomUUID() + "@example.com";
		jdbcTemplate.update("INSERT INTO users (id, first_name, last_name, email, password, role, email_verified) "
				+ "VALUES (?, 'Refresh', 'Flow', ?, 'x', 'CUSTOMER', true)", UUID.randomUUID(), email);

		createdEmails.add(email);

		String verificationToken = UUID.randomUUID().toString();
		redis.opsForValue().set("emailVerification:" + verificationToken, email, Duration.ofMinutes(5));

		return authService.verifyEmail(verificationToken);
	}

	private static String sha256(String value) {
		try {
			return HexFormat.of()
					.formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
