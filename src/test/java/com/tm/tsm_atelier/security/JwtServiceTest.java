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
		@DisplayName("Deve gerar um token JWT válido")
		void shouldGenerateValidJwtToken() {
			// Act
			String token = jwtService.generateToken(userDetails);

			// Assert
			assertThat(token).isNotBlank();
			// Um JWT válido possui três partes separadas por ponto
			assertThat(token.split("\\.")).hasSize(3);
		}

		@Test
		@DisplayName("Deve incluir o email do usuário como subject do token")
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
		@DisplayName("Deve extrair o email corretamente do token")
		void shouldExtractEmailFromToken() {
			// Arrange
			String token = jwtService.generateToken(userDetails);

			// Act
			String extractedUsername = jwtService.extractUsername(token);

			// Assert
			assertThat(extractedUsername).isEqualTo("test@example.com");
		}

		@Test
		@DisplayName("Deve lançar exceção quando a assinatura for inválida")
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
		@DisplayName("Deve retornar true quando o token é válido e pertence ao usuário")
		void shouldReturnTrueWhenTokenIsValidAndBelongsToUser() {
			// Arrange
			String token = jwtService.generateToken(userDetails);

			// Act
			boolean isValid = jwtService.isTokenValid(token, userDetails.getUsername());

			// Assert
			assertThat(isValid).isTrue();
		}

		@Test
		@DisplayName("Deve retornar false quando o token pertence a outro usuário")
		void shouldReturnFalseWhenTokenBelongsToAnotherUser() {
			// Arrange
			String token = jwtService.generateToken(userDetails);

			boolean isValid = jwtService.isTokenValid(token, "another@example.com");

			// Assert
			assertThat(isValid).isFalse();
		}

		@Test
		@DisplayName("Deve lançar exceção quando o token está expirado")
		void shouldThrowWhenTokenIsExpired() {
			// Arrange
			// Simula a expiração usando Reflection para setar um tempo de expiração no
			// passado
			ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
			String expiredToken = jwtService.generateToken(userDetails);

			// Act & Assert
			// O parser do io.jsonwebtoken já lança ExpiredJwtException automaticamente
			// quando tenta extrair informações de um token com data no passado
			assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, userDetails.getUsername()))
					.isInstanceOf(ExpiredJwtException.class);
		}
	}
}
