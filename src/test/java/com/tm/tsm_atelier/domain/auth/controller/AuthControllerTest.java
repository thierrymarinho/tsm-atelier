package com.tm.tsm_atelier.domain.auth.controller;

import static com.tm.tsm_atelier.common.builders.AuthResponseDTOBuilder.anAuthResponseDTO;
import static com.tm.tsm_atelier.common.builders.LoginRequestBuilder.aLoginRequest;
import static com.tm.tsm_atelier.common.builders.RegisterRequestBuilder.aRegisterRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tm.tsm_atelier.common.exception.custom.EmailAlreadyExistsException;
import com.tm.tsm_atelier.domain.auth.controller.v1.AuthController;
import com.tm.tsm_atelier.domain.auth.dto.AuthResponseDTO;
import com.tm.tsm_atelier.domain.auth.dto.LoginRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterResponseDTO;
import com.tm.tsm_atelier.domain.auth.dto.VerifyEmailRequestDTO;
import com.tm.tsm_atelier.domain.auth.service.AuthService;
import com.tm.tsm_atelier.domain.user.dto.UserResponseDTO;
import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import com.tm.tsm_atelier.security.JwtService;
import com.tm.tsm_atelier.security.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private com.tm.tsm_atelier.security.RateLimitService rateLimitService;

	private static final String BASE_URL = "/api/v1/auth";

	private ResultActions performRegister(RegisterRequestDTO request) throws Exception {
		return mockMvc.perform(post(BASE_URL + "/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));
	}

	private ResultActions performVerifyEmail(String token) throws Exception {
		return mockMvc.perform(post(BASE_URL + "/verify-email").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new VerifyEmailRequestDTO(token))));
	}

	private ResultActions performLogin(LoginRequestDTO request) throws Exception {
		return mockMvc.perform(post(BASE_URL + "/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));
	}

	@Nested
	@DisplayName("POST /register")
	class Register {

		@Test
		@DisplayName("Should return 201 and the verification message when the registration is valid")
		void shouldReturn201WithMessageWhenRegistrationIsValid() throws Exception {
			// Arrange
			RegisterRequestDTO request = aRegisterRequest().build();
			RegisterResponseDTO mockResponse = new RegisterResponseDTO(
					"Registration successful. Please check your email to verify your account.");

			when(authService.register(any(RegisterRequestDTO.class))).thenReturn(mockResponse);

			// Act & Assert
			performRegister(request).andExpect(status().isCreated())
					.andExpect(jsonPath("$.message").value(mockResponse.message()))
					.andExpect(cookie().doesNotExist("access_token")).andExpect(cookie().doesNotExist("refresh_token"));

			verify(authService).register(any(RegisterRequestDTO.class));
		}

		@Test
		@DisplayName("Should return 422 when the email is invalid")
		void shouldReturn422WhenEmailIsInvalid() throws Exception {
			// Arrange
			RegisterRequestDTO request = aRegisterRequest().withEmail("email-invalido").build();

			// Act & Assert
			performRegister(request).andExpect(status().isUnprocessableContent())
					.andExpect(jsonPath("$.status").value(422))
					.andExpect(jsonPath("$.fields.email").value("Invalid email format"));

			verify(authService, never()).register(any(RegisterRequestDTO.class));
		}

		@Test
		@DisplayName("Should return 422 when required fields are blank")
		void shouldReturn422WhenRequiredFieldsAreBlank() throws Exception {
			// Arrange
			RegisterRequestDTO request = aRegisterRequest().withFirstName("").withPassword("").build();

			// Act & Assert
			performRegister(request).andExpect(status().isUnprocessableContent())
					.andExpect(jsonPath("$.fields.firstName").exists())
					.andExpect(jsonPath("$.fields.password").exists());

			verify(authService, never()).register(any(RegisterRequestDTO.class));
		}

		@Test
		@DisplayName("Should return 409 when the email is already in use")
		void shouldReturn409WhenEmailAlreadyExists() throws Exception {
			// Arrange
			RegisterRequestDTO request = aRegisterRequest().build();

			when(authService.register(any(RegisterRequestDTO.class)))
					.thenThrow(new EmailAlreadyExistsException("Email is already in use."));

			// Act & Assert
			performRegister(request).andExpect(status().isConflict()) // 409
					.andExpect(jsonPath("$.detail").value("Email is already in use."));

			verify(authService).register(any(RegisterRequestDTO.class));
		}

		@Test
		@DisplayName("Should return 422 when the password is shorter than the minimum")
		void shouldReturn422WhenPasswordIsTooShort() throws Exception {
			// Arrange
			RegisterRequestDTO request = aRegisterRequest().withPassword("12").build();

			// Act & Assert
			performRegister(request).andExpect(status().isUnprocessableContent())
					.andExpect(jsonPath("$.status").value(422))
					.andExpect(jsonPath("$.fields.password").value("Password must be between 8 and 72 characters"));

			verify(authService, never()).register(any(RegisterRequestDTO.class));
		}
	}

	@Nested
	@DisplayName("POST /login")
	class Login {

		@Test
		@DisplayName("Should return 200 and set the cookies when the credentials are valid")
		void shouldReturn200AndSetCookiesWhenCredentialsAreValid() throws Exception {
			// Arrange
			LoginRequestDTO request = aLoginRequest().build();
			AuthResponseDTO mockTokens = anAuthResponseDTO().build();

			when(authService.login(any(LoginRequestDTO.class), anyString())).thenReturn(mockTokens);
			// Act & Assert
			performLogin(request).andExpect(status().isOk()).andExpect(cookie().exists("access_token"))
					.andExpect(cookie().value("access_token", mockTokens.accessToken()))
					.andExpect(cookie().exists("refresh_token"))
					.andExpect(cookie().value("refresh_token", mockTokens.refreshToken()))
					.andExpect(jsonPath("$.email").value(mockTokens.email()))
					.andExpect(jsonPath("$.name").value(mockTokens.name()));

			verify(authService).login(any(LoginRequestDTO.class), anyString());
		}

		@Test
		@DisplayName("Should return 401 when the credentials are invalid")
		void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
			// Arrange
			LoginRequestDTO request = aLoginRequest().build();

			when(authService.login(any(LoginRequestDTO.class), anyString()))
					.thenThrow(new BadCredentialsException("Invalid email or password."));

			// Act & Assert
			performLogin(request).andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.title").value("Authentication failed"))
					.andExpect(jsonPath("$.detail").value("Invalid email or password."));

			verify(authService).login(any(LoginRequestDTO.class), anyString());
		}

		@Test
		@DisplayName("Should set the access_token cookie as HttpOnly")
		void shouldSetAccessTokenCookieAsHttpOnly() throws Exception {
			LoginRequestDTO request = aLoginRequest().build();
			AuthResponseDTO mockTokens = anAuthResponseDTO().build();

			when(authService.login(any(LoginRequestDTO.class), anyString())).thenReturn(mockTokens);

			// Act & Assert
			performLogin(request).andExpect(status().isOk()).andExpect(cookie().exists("access_token"))
					.andExpect(cookie().value("access_token", mockTokens.accessToken()))
					.andExpect(cookie().httpOnly("access_token", true)).andExpect(cookie().secure("access_token", true))
					.andExpect(cookie().exists("refresh_token"))
					.andExpect(cookie().value("refresh_token", mockTokens.refreshToken()))
					.andExpect(cookie().httpOnly("refresh_token", true))
					.andExpect(cookie().secure("refresh_token", true));
		}

		@Test
		@DisplayName("Should set the refresh_token cookie with a restricted path")
		void shouldSetRefreshTokenCookieWithRestrictedPath() throws Exception {
			// Arrange
			LoginRequestDTO request = aLoginRequest().build();
			AuthResponseDTO mockTokens = anAuthResponseDTO().build();

			when(authService.login(any(LoginRequestDTO.class), anyString())).thenReturn(mockTokens);

			// Act & Assert
			performLogin(request).andExpect(status().isOk()).andExpect(cookie().path("refresh_token", "/api/v1/auth"));
		}
	}

	@Nested
	@DisplayName("POST /refresh")
	class Refresh {

		@Test
		@DisplayName("Should return 200 and set new cookies when the refresh token is valid")
		void shouldReturn200AndSetNewCookiesWhenRefreshTokenIsValid() throws Exception {
			// Arrange
			String validToken = "valid-refresh-token";
			AuthResponseDTO newTokens = anAuthResponseDTO().withAccessToken("new-access")
					.withRefreshToken("new-refresh").build();

			when(authService.refresh(validToken)).thenReturn(newTokens);

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/refresh").with(csrf())
					.cookie(new jakarta.servlet.http.Cookie("refresh_token", validToken))).andExpect(status().isOk())
					.andExpect(cookie().exists("access_token")).andExpect(cookie().value("access_token", "new-access"))
					.andExpect(cookie().exists("refresh_token"))
					.andExpect(cookie().value("refresh_token", "new-refresh"))
					.andExpect(jsonPath("$.email").value(newTokens.email()))
					.andExpect(jsonPath("$.name").value(newTokens.name()));

			verify(authService).refresh(validToken);
		}

		@Test
		@DisplayName("Should return 401 when there is no refresh token cookie")
		void shouldReturn401WhenNoRefreshTokenCookie() throws Exception {
			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/refresh").with(csrf())).andExpect(status().isUnauthorized());

			verify(authService, never()).refresh(anyString());
		}

		@Test
		@DisplayName("Should return 401 when the refresh token is invalid")
		void shouldReturn401WhenRefreshTokenIsInvalid() throws Exception {
			// Arrange
			String invalidToken = "invalid-refresh-token";

			when(authService.refresh(invalidToken))
					.thenThrow(new com.tm.tsm_atelier.common.exception.custom.InvalidTokenException("Invalid token"));

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/refresh").with(csrf())
					.cookie(new jakarta.servlet.http.Cookie("refresh_token", invalidToken)))
					.andExpect(status().isUnauthorized());

			verify(authService).refresh(invalidToken);
		}
	}

	@Nested
	@DisplayName("GET /me")
	class Me {

		@Test
		@DisplayName("Should return 200 and the user data when authenticated via the access_token cookie")
		void shouldReturn200AndUserProfileWhenAuthenticatedViaCookie() throws Exception {
			// Arrange
			String token = "valid-access-token";
			UserResponseDTO profile = new UserResponseDTO(UUID.randomUUID(), "Maria", "Silva", "Maria Silva",
					"user@example.com", Role.CUSTOMER);

			when(jwtService.extractUsername(anyString())).thenReturn("user@example.com");
			when(jwtService.extractRole(anyString())).thenReturn("ROLE_CUSTOMER");
			when(jwtService.isTokenValid(anyString(), anyString())).thenReturn(true);
			when(authService.getMe("user@example.com")).thenReturn(profile);

			// Act & Assert
			mockMvc.perform(get(BASE_URL + "/me").cookie(new jakarta.servlet.http.Cookie("access_token", token)))
					.andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Maria"))
					.andExpect(jsonPath("$.lastName").value("Silva")).andExpect(jsonPath("$.name").value("Maria Silva"))
					.andExpect(jsonPath("$.email").value("user@example.com"))
					.andExpect(jsonPath("$.role").value("CUSTOMER"));

			verify(authService).getMe("user@example.com");
		}

		@Test
		@DisplayName("Should return 200 and the user data when authenticated via the Authorization Bearer header")
		void shouldReturn200AndUserProfileWhenAuthenticatedViaHeader() throws Exception {
			// Arrange
			String token = "valid-access-token";
			UserResponseDTO profile = new UserResponseDTO(UUID.randomUUID(), "Maria", "Silva", "Maria Silva",
					"user@example.com", Role.CUSTOMER);

			when(jwtService.extractUsername(anyString())).thenReturn("user@example.com");
			when(jwtService.extractRole(anyString())).thenReturn("ROLE_CUSTOMER");
			when(jwtService.isTokenValid(anyString(), anyString())).thenReturn(true);
			when(authService.getMe("user@example.com")).thenReturn(profile);

			// Act & Assert
			mockMvc.perform(get(BASE_URL + "/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
					.andExpect(jsonPath("$.firstName").value("Maria")).andExpect(jsonPath("$.lastName").value("Silva"))
					.andExpect(jsonPath("$.name").value("Maria Silva"))
					.andExpect(jsonPath("$.email").value("user@example.com"))
					.andExpect(jsonPath("$.role").value("CUSTOMER"));

			verify(authService).getMe("user@example.com");
		}

		@Test
		@DisplayName("Should return 401 when not authenticated")
		void shouldReturn401WhenNotAuthenticated() throws Exception {
			// Act & Assert
			mockMvc.perform(get(BASE_URL + "/me")).andExpect(status().isUnauthorized());

			verify(authService, never()).getMe(anyString());
		}
	}

	@Nested
	@DisplayName("POST /logout")
	class Logout {

		@Test
		@DisplayName("Should return 200 and clear the cookies")
		void shouldReturn200AndClearCookies() throws Exception {
			// Arrange
			String token = "any-token";

			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/logout").with(csrf())
					.cookie(new jakarta.servlet.http.Cookie("refresh_token", token))).andExpect(status().isOk())
					.andExpect(cookie().value("access_token", "")).andExpect(cookie().value("refresh_token", ""));

			verify(authService).logout(token);
		}

		@Test
		@DisplayName("Should set maxAge=0 on the cookies to force removal in the browser")
		void shouldSetMaxAgeZeroOnCookies() throws Exception {
			// Act & Assert
			mockMvc.perform(post(BASE_URL + "/logout").with(csrf())).andExpect(status().isOk())
					.andExpect(cookie().maxAge("access_token", 0)).andExpect(cookie().maxAge("refresh_token", 0));

			verify(authService).logout(null);
		}
	}

	@Nested
	@DisplayName("POST /verify-email")
	class VerifyEmail {

		@Test
		@DisplayName("Should return 200 and set the JWT cookies when the verification token is valid")
		void shouldReturn200AndSetCookiesWhenTokenIsValid() throws Exception {
			// Arrange
			String token = "valid-verification-token";
			AuthResponseDTO mockTokens = anAuthResponseDTO().build();

			when(authService.verifyEmail(token)).thenReturn(mockTokens);

			// Act & Assert
			performVerifyEmail(token).andExpect(status().isOk()).andExpect(cookie().exists("access_token"))
					.andExpect(cookie().value("access_token", mockTokens.accessToken()))
					.andExpect(cookie().exists("refresh_token"))
					.andExpect(cookie().value("refresh_token", mockTokens.refreshToken()));

			verify(authService).verifyEmail(token);
		}

		@Test
		@DisplayName("Should return 401 when the verification token is invalid or expired")
		void shouldReturn401WhenTokenIsInvalid() throws Exception {
			// Arrange
			String invalidToken = "invalid-token";

			when(authService.verifyEmail(invalidToken))
					.thenThrow(new com.tm.tsm_atelier.common.exception.custom.InvalidTokenException(
							"Invalid or expired verification token."));

			// Act & Assert
			performVerifyEmail(invalidToken).andExpect(status().isUnauthorized());

			verify(authService).verifyEmail(invalidToken);
		}
	}
}
