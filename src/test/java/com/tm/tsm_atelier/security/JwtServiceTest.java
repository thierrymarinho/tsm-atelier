package com.tm.tsm_atelier.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

	private JwtService jwtService;
	private UserDetails userDetails;

	// Chave secreta de pelo menos 256 bits exigida pelo algoritmo HS256
	private static final String SECRET = "minhaChaveSuperSecretaMuitoLongaParaOJwtFuncionarCorretamente";
	private static final long EXPIRATION = 1000 * 60 * 60; // 1 hora

	@BeforeEach
	void setUp() {
		jwtService = new JwtService();

		// Injeta os valores através do ReflectionTestUtils (substitui o @Value)
		ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
		ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);

		userDetails = mock(UserDetails.class);
		when(userDetails.getUsername()).thenReturn("test@example.com");
	}

	@Nested
	@DisplayName("generateToken()")
	class GenerateToken {

		@Test
		@DisplayName("Should generate a valid JWT")
		void shouldGenerateValidJwtToken() {
			// Act
			String token = jwtService.generateToken(userDetails);

			// Assert
			assertThat(token).isNotBlank();
			// Um JWT válido possui três partes separadas por ponto
			assertThat(token.split("\\.")).hasSize(3);
		}

		@Test
		@DisplayName("Should use the user email as the token subject")
		void shouldIncludeUserEmailAsSubject() {
			// Act
			String token = jwtService.generateToken(userDetails);
			String subject = jwtService.extractUsername(token);

			// Assert
			assertThat(subject).isEqualTo("test@example.com");
		}
	}

	@Nested
	@DisplayName("extractUsername()")
	class ExtractUsername {

		@Test
		@DisplayName("Should extract the email from the token correctly")
		void shouldExtractEmailFromToken() {
			// Arrange
			String token = jwtService.generateToken(userDetails);

			// Act
			String extractedUsername = jwtService.extractUsername(token);

			// Assert
			assertThat(extractedUsername).isEqualTo("test@example.com");
		}

		@Test
		@DisplayName("Should throw when the signature is invalid")
		void shouldThrowExceptionWhenSignatureIsInvalid() {
			// Arrange
			String token = jwtService.generateToken(userDetails) + "invalid";

			// Act & Assert
			assertThatThrownBy(() -> jwtService.extractUsername(token)).isInstanceOf(SignatureException.class);
		}
	}

	@Nested
	@DisplayName("isTokenValid()")
	class IsTokenValid {

		@Test
		@DisplayName("Should return true when the token is valid and belongs to the user")
		void shouldReturnTrueWhenTokenIsValidAndBelongsToUser() {
			// Arrange
			String token = jwtService.generateToken(userDetails);

			// Act
			boolean isValid = jwtService.isTokenValid(token, userDetails.getUsername());

			// Assert
			assertThat(isValid).isTrue();
		}

		@Test
		@DisplayName("Should return false when the token belongs to another user")
		void shouldReturnFalseWhenTokenBelongsToAnotherUser() {
			// Arrange
			String token = jwtService.generateToken(userDetails);

			boolean isValid = jwtService.isTokenValid(token, "another@example.com");

			// Assert
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("Should throw when the token is expired")
		void shouldThrowWhenTokenIsExpired() {
			// Arrange
			ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
			String expiredToken = jwtService.generateToken(userDetails);

			// Act & Assert

			assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, userDetails.getUsername()))
					.isInstanceOf(ExpiredJwtException.class);
		}
	}

	/**
	 * A validação roda na subida da aplicação. Estes testes existem porque o
	 * segredo anterior estava versionado e a aplicação subia com ele em silêncio —
	 * o que só apareceria como conta invadida, nunca como erro.
	 */
	@Nested
	@DisplayName("validateSecret()")
	class ValidateSecret {

		@Test
		@DisplayName("Should accept a secret long enough for HS256")
		void shouldAcceptASecretLongEnoughForHs256() {
			ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);

			jwtService.validateSecret();
		}

		@Test
		@DisplayName("Should reject a missing secret")
		void shouldRejectAMissingSecret() {
			ReflectionTestUtils.setField(jwtService, "secretKey", "   ");

			assertThatThrownBy(() -> jwtService.validateSecret()).isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("JWT_SECRET is required");
		}

		@Test
		@DisplayName("Should reject a secret shorter than 256 bits")
		void shouldRejectASecretShorterThan256Bits() {
			ReflectionTestUtils.setField(jwtService, "secretKey", "too-short");

			assertThatThrownBy(() -> jwtService.validateSecret()).isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("at least 32 bytes");
		}
	}
}
