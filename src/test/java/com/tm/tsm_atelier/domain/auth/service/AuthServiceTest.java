package com.tm.tsm_atelier.domain.auth.service;

import static com.tm.tsm_atelier.common.builders.LoginRequestBuilder.aLoginRequest;
import static com.tm.tsm_atelier.common.builders.RegisterRequestBuilder.aRegisterRequest;
import static com.tm.tsm_atelier.common.builders.UserBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.tm.tsm_atelier.common.exception.custom.EmailAlreadyExistsException;
import com.tm.tsm_atelier.common.exception.custom.EmailNotVerifiedException;
import com.tm.tsm_atelier.common.exception.custom.InvalidTokenException;
import com.tm.tsm_atelier.common.exception.custom.UserNotFoundException;
import com.tm.tsm_atelier.domain.auth.dto.AuthResponseDTO;
import com.tm.tsm_atelier.domain.auth.dto.LoginRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterResponseDTO;
import com.tm.tsm_atelier.domain.common.port.EmailPort;
import com.tm.tsm_atelier.domain.user.dto.UserResponseDTO;
import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import com.tm.tsm_atelier.security.JwtService;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@InjectMocks
	private AuthService authService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private SetOperations<String, String> setOperations;

	@Mock
	private EmailPort emailService;

	@Mock
	private com.tm.tsm_atelier.security.RateLimitService rateLimitService;

	@Captor
	private ArgumentCaptor<User> userCaptor;

	@Nested
	@DisplayName("register()")
	class Register {

		@Test
		@DisplayName("Deve registrar um novo usuário, enviar email de verificação e retornar mensagem")
		void shouldRegisterNewUserAndSendVerificationEmail() {
			// Arrange
			ReflectionTestUtils.setField(authService, "emailVerificationExpiration", 86400000L);
			ReflectionTestUtils.setField(authService, "appBaseUrl", "http://localhost:3000");

			RegisterRequestDTO request = aRegisterRequest().build();

			when(userRepository.existsByEmail(anyString())).thenReturn(false);
			when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
			when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
			when(redisTemplate.opsForValue()).thenReturn(valueOperations);

			// Act
			RegisterResponseDTO result = authService.register(request);

			// Assert — retorna mensagem em vez de JWT
			assertThat(result).isNotNull();
			assertThat(result.message()).isNotBlank();

			// Verifica que o usuário foi salvo com emailVerified=false
			verify(userRepository, times(1)).save(userCaptor.capture());
			User savedUser = userCaptor.getValue();
			assertThat(savedUser.getFirstName()).isEqualTo(request.firstName());
			assertThat(savedUser.getLastName()).isEqualTo(request.lastName());
			assertThat(savedUser.getEmail()).isEqualTo(request.email());
			assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
			assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
			assertThat(savedUser.isEmailVerified()).isFalse();

			// Verifica que o token de verificação foi salvo no Redis
			verify(redisTemplate, times(1)).opsForValue();
			verify(valueOperations).set(anyString(), eq(request.email()), any(Duration.class));

			// Verifica que o email foi disparado (assíncrono, mas chamado)
			verify(emailService).sendVerificationEmail(eq(request.email()), eq(request.firstName()), anyString());

			// Verifica que NÃO gerou JWT
			verify(jwtService, never()).generateToken(any());
		}

		@Test
		@DisplayName("Deve lançar exceção quando o email já está em uso")
		void shouldThrowWhenEmailAlreadyExists() {
			// Arrange
			RegisterRequestDTO request = aRegisterRequest().build();

			// Act
			when(userRepository.existsByEmail(request.email())).thenReturn(true);

			// Assert
			assertThatThrownBy(() -> authService.register(request)).isInstanceOf(EmailAlreadyExistsException.class)
					.hasMessage("Email is already in use.");

			verify(userRepository, never()).save(any());
			verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
		}
	}

	@Nested
	@DisplayName("verifyEmail()")
	class VerifyEmail {

		@Test
		@DisplayName("Deve verificar o email e retornar JWT quando o token é válido")
		void shouldVerifyEmailAndReturnTokensWhenTokenIsValid() {
			// Arrange
			String token = UUID.randomUUID().toString();
			String redisKey = "emailVerification:" + token;
			User user = aUser().withEmail("user@email.com").build();

			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.get(redisKey)).thenReturn(user.getEmail());
			when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
			when(jwtService.generateToken(user)).thenReturn("new-jwt-token");
			when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

			ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);
			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(redisTemplate.opsForSet()).thenReturn(setOperations);

			// Act
			AuthResponseDTO result = authService.verifyEmail(token);

			// Assert
			assertThat(result).isNotNull();
			assertThat(result.accessToken()).isEqualTo("new-jwt-token");
			assertThat(result.refreshToken()).isNotBlank();

			// Verifica que o usuário foi marcado como verificado
			verify(userRepository).save(userCaptor.capture());
			assertThat(userCaptor.getValue().isEmailVerified()).isTrue();

			// Verifica que o token de verificação foi removido do Redis
			verify(redisTemplate).delete(redisKey);
		}

		@Test
		@DisplayName("Deve lançar exceção quando o token de verificação é inválido ou expirado")
		void shouldThrowWhenVerificationTokenIsInvalid() {
			// Arrange
			String invalidToken = "invalid-token";
			String redisKey = "emailVerification:" + invalidToken;

			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.get(redisKey)).thenReturn(null);

			// Act & Assert
			assertThatThrownBy(() -> authService.verifyEmail(invalidToken)).isInstanceOf(InvalidTokenException.class)
					.hasMessage("Invalid or expired verification token.");

			verify(userRepository, never()).save(any());
			verify(jwtService, never()).generateToken(any());
		}
	}

	@Nested
	@DisplayName("login()")
	class Login {

		@Test
		@DisplayName("Deve retornar tokens quando as credenciais são válidas e email está verificado")
		void shouldReturnTokensWhenCredentialsAreValid() {
			// 1. Arrange
			ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);
			LoginRequestDTO login = aLoginRequest().build();
			// Cria usuário com email verificado
			User user = aUser().withEmail(login.email()).withEmailVerified(true).build();

			when(userRepository.findByEmail(login.email())).thenReturn(Optional.of(user));
			when(jwtService.generateToken(user)).thenReturn("fake-jwt-token");
			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(redisTemplate.opsForSet()).thenReturn(setOperations);

			// 2. Act
			AuthResponseDTO result = authService.login(login);

			// 3. Assert
			assertThat(result).isNotNull();
			assertThat(result.accessToken()).isEqualTo("fake-jwt-token");
			assertThat(result.refreshToken()).isNotBlank();
			assertThat(result.email()).isEqualTo(user.getEmail());

			verify(authenticationManager).authenticate(any());
			verify(redisTemplate).opsForValue();
			verify(valueOperations).set(startsWith("rt:valid:"), eq(user.getEmail()), any(Duration.class));
			verify(redisTemplate).opsForSet();
			verify(setOperations).add(eq("rt:user:" + user.getEmail()), anyString());
		}

		@Test
		@DisplayName("Deve lançar EmailNotVerifiedException quando o email não foi verificado")
		void shouldThrowWhenEmailIsNotVerified() {
			// Arrange
			LoginRequestDTO login = aLoginRequest().build();
			// Cria usuário com email NÃO verificado
			User user = aUser().withEmail(login.email()).withEmailVerified(false).build();

			when(userRepository.findByEmail(login.email())).thenReturn(Optional.of(user));

			// Act & Assert
			assertThatThrownBy(() -> authService.login(login)).isInstanceOf(EmailNotVerifiedException.class)
					.hasMessage("Please verify your email before logging in.");

			verify(jwtService, never()).generateToken(any());
		}

		@Test
		@DisplayName("Deve lançar exceção quando as credenciais são inválidas")
		void shouldThrowBadCredentialsWhenCredentialsAreInvalid() {
			// Arrange
			LoginRequestDTO request = aLoginRequest().build();

			when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));
			// Act & Assert
			assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class)
					.hasMessage("Bad credentials");

			verify(userRepository, never()).findByEmail(anyString());
		}
	}

	@Nested
	@DisplayName("refresh()")
	class Refresh {

		@Test
		@DisplayName("Deve retornar novos tokens e dados do usuário quando o refresh token é válido")
		void shouldReturnNewTokensWhenRefreshTokenIsValid() {
			// Arrange
			ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);
			String oldRefreshToken = UUID.randomUUID().toString();
			User user = aUser().withEmail("user@email.com").build();

			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(redisTemplate.opsForSet()).thenReturn(setOperations);
			when(valueOperations.get(startsWith("rt:valid:"))).thenReturn(user.getEmail());
			when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
			when(jwtService.generateToken(user)).thenReturn("new-jwt-token");

			// Act
			AuthResponseDTO result = authService.refresh(oldRefreshToken);

			// Assert
			assertThat(result).isNotNull();
			assertThat(result.accessToken()).isEqualTo("new-jwt-token");
			assertThat(result.refreshToken()).isNotBlank().isNotEqualTo(oldRefreshToken);
			assertThat(result.email()).isEqualTo(user.getEmail());

			verify(valueOperations).get(startsWith("rt:valid:"));
			verify(jwtService).generateToken(user);
			verify(redisTemplate).delete(startsWith("rt:valid:"));
			verify(setOperations).remove(eq("rt:user:" + user.getEmail()), anyString());
			verify(valueOperations).set(startsWith("rt:used:"), eq(user.getEmail()), any(Duration.class));
			verify(valueOperations, times(2)).set(anyString(), eq(user.getEmail()), any(Duration.class));
		}

		@Test
		@DisplayName("Deve lançar exceção quando o refresh token é inválido ou expirado")
		void shouldThrowWhenRefreshTokenIsInvalidOrExpired() {
			// Arrange
			String invalidToken = "invalid-token";

			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.get(startsWith("rt:valid:"))).thenReturn(null);
			when(valueOperations.get(startsWith("rt:used:"))).thenReturn(null);

			// Act & Assert
			assertThatThrownBy(() -> authService.refresh(invalidToken))
					.isInstanceOf(com.tm.tsm_atelier.common.exception.custom.InvalidTokenException.class)
					.hasMessage("Invalid or expired refresh token.");

			verify(jwtService, never()).generateToken(any());
			verify(redisTemplate, never()).delete(anyString());
		}

		@Test
		@DisplayName("Deve revogar todos os tokens do usuário quando detectar reuso do refresh token")
		void shouldRevokeAllTokensWhenTokenReuseDetected() {
			// Arrange
			String reusedToken = "reused-token";
			String victimEmail = "victim@email.com";
			java.util.Set<String> activeHashes = java.util.Set.of("hash1", "hash2");

			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.get(startsWith("rt:valid:"))).thenReturn(null);
			when(valueOperations.get(startsWith("rt:used:"))).thenReturn(victimEmail);

			when(redisTemplate.opsForSet()).thenReturn(setOperations);
			when(setOperations.members("rt:user:" + victimEmail)).thenReturn(activeHashes);

			// Act & Assert
			assertThatThrownBy(() -> authService.refresh(reusedToken))
					.isInstanceOf(com.tm.tsm_atelier.common.exception.custom.InvalidTokenException.class)
					.hasMessage("Security Alert: Token reuse detected. All sessions have been revoked.");

			verify(redisTemplate).delete("rt:valid:hash1");
			verify(redisTemplate).delete("rt:valid:hash2");
			verify(redisTemplate).delete("rt:user:" + victimEmail);
			verify(jwtService, never()).generateToken(any());
		}
	}

	@Nested
	@DisplayName("getMe()")
	class GetMe {

		@Test
		@DisplayName("Deve retornar os dados do usuário buscando pelo email")
		void shouldReturnUserProfileByEmail() {
			// Arrange
			User user = aUser().withEmail("user@email.com").build();
			when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(user));

			// Act
			UserResponseDTO result = authService.getMe("user@email.com");

			// Assert
			assertThat(result).isNotNull();
			assertThat(result.email()).isEqualTo("user@email.com");
			assertThat(result.firstName()).isEqualTo(user.getFirstName());
			assertThat(result.lastName()).isEqualTo(user.getLastName());
			assertThat(result.name()).isEqualTo(user.getFirstName() + " " + user.getLastName());

			verify(userRepository).findByEmail("user@email.com");
		}

		@Test
		@DisplayName("Deve lançar exceção quando usuário não for encontrado")
		void shouldThrowWhenUserNotFound() {
			// Arrange
			when(userRepository.findByEmail("notfound@email.com")).thenReturn(Optional.empty());

			// Act & Assert
			assertThatThrownBy(() -> authService.getMe("notfound@email.com"))
					.isInstanceOf(UserNotFoundException.class)
					.hasMessage("User not found.");
		}
	}

	@Nested
	@DisplayName("logout()")
	class Logout {

		@Test
		@DisplayName("Deve apagar o refresh token do Redis")
		void shouldDeleteRefreshTokenFromRedis() {
			// Arrange
			String tokenToRevoke = "my-valid-token";
			String email = "user@example.com";

			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.get(startsWith("rt:valid:"))).thenReturn(email);
			when(redisTemplate.opsForSet()).thenReturn(setOperations);

			// Act
			authService.logout(tokenToRevoke);

			// Assert
			verify(redisTemplate).delete(startsWith("rt:valid:"));
			verify(setOperations).remove(eq("rt:user:" + email), anyString());
		}

		@Test
		@DisplayName("Deve lidar com refresh token nulo sem lançar exceção")
		void shouldHandleNullRefreshTokenGracefully() {
			// Act
			authService.logout(null);

			// Assert
			verify(redisTemplate, never()).delete(anyString());
		}
	}
}
