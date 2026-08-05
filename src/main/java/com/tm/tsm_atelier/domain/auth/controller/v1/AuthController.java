package com.tm.tsm_atelier.domain.auth.controller.v1;

import com.tm.tsm_atelier.common.exception.custom.EmailNotVerifiedException;
import com.tm.tsm_atelier.common.exception.custom.InvalidTokenException;
import com.tm.tsm_atelier.common.exception.custom.UserNotFoundException;
import com.tm.tsm_atelier.domain.auth.dto.AuthResponseDTO;
import com.tm.tsm_atelier.domain.auth.dto.LoginRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RefreshRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterResponseDTO;
import com.tm.tsm_atelier.domain.auth.dto.ResendVerificationRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.VerifyEmailRequestDTO;
import com.tm.tsm_atelier.domain.auth.service.AuthService;
import com.tm.tsm_atelier.domain.user.dto.UserResponseDTO;
import com.tm.tsm_atelier.security.RateLimitService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final RateLimitService rateLimitService;

	@Value("${jwt.access-token-expiration}")
	private long accessTokenExpiration;

	@Value("${jwt.refresh-token-expiration}")
	private long refreshTokenExpiration;

	public AuthController(AuthService authService, RateLimitService rateLimitService) {
		this.authService = authService;
		this.rateLimitService = rateLimitService;
	}

	@PostMapping("/register")
	public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request,
			HttpServletRequest httpRequest) {
		rateLimitService.checkRateLimit("register", getClientIp(httpRequest), 5, Duration.ofMinutes(15));
		RegisterResponseDTO response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/verify-email")
	public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequestDTO request,
			HttpServletResponse response) {
		AuthResponseDTO tokens = authService.verifyEmail(request.token());
		addCookiesToResponse(response, tokens);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/resend-verification")
	public ResponseEntity<Void> resendVerificationEmail(@Valid @RequestBody ResendVerificationRequestDTO request,
			HttpServletRequest httpRequest) {
		rateLimitService.checkRateLimit("resend-verification", getClientIp(httpRequest), 3, Duration.ofMinutes(15));
		authService.resendVerificationEmail(request.email());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request,
			HttpServletRequest httpRequest, HttpServletResponse response) {
		String clientIp = getClientIp(httpRequest);
		rateLimitService.checkRateLimit("login", clientIp, 10, Duration.ofMinutes(15));
		AuthResponseDTO tokens = authService.login(request, clientIp);
		addCookiesToResponse(response, tokens);
		return ResponseEntity.ok(tokens);
	}

	/**
	 * O corpo é opcional: o cookie httpOnly continua sendo o caminho do SPA e tem
	 * prioridade zero — só é lido quando não veio token no corpo. Antes o cookie
	 * era a única origem aceita, então o refreshToken devolvido no JSON do login
	 * não servia para nada em cliente sem cookie jar.
	 */
	@PostMapping("/refresh")
	public ResponseEntity<Object> refresh(@RequestBody(required = false) RefreshRequestDTO body,
			HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = body != null && body.refreshToken() != null && !body.refreshToken().isBlank()
				? body.refreshToken()
				: extractRefreshToken(request);

		if (refreshToken == null) {
			return unauthorized(response, "No refresh token was provided.");
		}

		try {
			AuthResponseDTO newTokens = authService.refresh(refreshToken);
			addCookiesToResponse(response, newTokens);
			return ResponseEntity.ok(newTokens);
		} catch (InvalidTokenException | UserNotFoundException | EmailNotVerifiedException e) {
			return unauthorized(response, e.getMessage());
		}
	}

	/**
	 * O 401 do refresh vinha sem corpo e sem mexer nos cookies: o frontend não
	 * distinguia "o token expirou, faça login" de "detectamos reuso e revogamos
	 * tudo", e o browser seguia mandando o token morto em toda tentativa seguinte.
	 */
	private ResponseEntity<Object> unauthorized(HttpServletResponse response, String detail) {
		response.addHeader(HttpHeaders.SET_COOKIE, createEmptyCookie("access_token"));
		response.addHeader(HttpHeaders.SET_COOKIE, createEmptyCookie("refresh_token"));

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
		problem.setTitle("Invalid token");

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
	}

	/**
	 * Sem fallback: /me deixou de ser rota pública, então o filtro já autenticou
	 * quem chega aqui. A validação manual de JWT que existia neste método era um
	 * segundo caminho de autenticação — qualquer regra nova adicionada ao filtro
	 * teria de ser lembrada aqui também, e não seria.
	 */
	@GetMapping("/me")
	public ResponseEntity<UserResponseDTO> getMe(Authentication authentication) {
		try {
			return ResponseEntity.ok(authService.getMe(authentication.getName()));
		} catch (UserNotFoundException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		authService.logout(extractAccessToken(request), extractRefreshToken(request));

		response.addHeader(HttpHeaders.SET_COOKIE, createEmptyCookie("access_token"));
		response.addHeader(HttpHeaders.SET_COOKIE, createEmptyCookie("refresh_token"));

		return ResponseEntity.ok().build();
	}

	private void addCookiesToResponse(HttpServletResponse response, AuthResponseDTO tokens) {
		ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.accessToken()).httpOnly(true)
				.secure(true) // NOTA: Mudar para true quando for fazer deploy em HTTPS (ex: Railway)
				.path("/").maxAge(accessTokenExpiration / 1000).sameSite("Lax").build();

		ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken()).httpOnly(true)
				.secure(true) // NOTA: Mudar para true em produção
				.path("/api/v1/auth").maxAge(refreshTokenExpiration / 1000).sameSite("Lax").build();

		response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
	}

	private String createEmptyCookie(String name) {
		return ResponseCookie.from(name, "").httpOnly(true).secure(true)
				.path(name.equals("refresh_token") ? "/api/v1/auth" : "/").maxAge(0).sameSite("Lax").build().toString();
	}

	/**
	 * Só o cookie, pelo mesmo motivo do JwtAuthenticationFilter: uma credencial,
	 * uma porta de entrada.
	 */
	private String extractAccessToken(HttpServletRequest request) {
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if ("access_token".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	private String extractRefreshToken(HttpServletRequest request) {
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if ("refresh_token".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	/**
	 * Ler X-Forwarded-For diretamente permitiria que qualquer cliente forjasse o
	 * header e gerasse uma chave nova de rate limit a cada requisição, anulando o
	 * limitador. Com server.forward-headers-strategy=FRAMEWORK o próprio Spring
	 * resolve o header apenas quando ele vem do proxy da plataforma, então
	 * getRemoteAddr() já devolve o IP real e confiável.
	 */
	private String getClientIp(HttpServletRequest request) {
		return request.getRemoteAddr();
	}
}
