package com.tm.tsm_atelier.domain.auth.controller.v1;

import com.tm.tsm_atelier.common.exception.custom.EmailNotVerifiedException;
import com.tm.tsm_atelier.common.exception.custom.InvalidTokenException;
import com.tm.tsm_atelier.common.exception.custom.UserNotFoundException;
import com.tm.tsm_atelier.domain.auth.dto.AuthResponseDTO;
import com.tm.tsm_atelier.domain.auth.dto.LoginRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterResponseDTO;
import com.tm.tsm_atelier.domain.auth.dto.ResendVerificationRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.VerifyEmailRequestDTO;
import com.tm.tsm_atelier.domain.auth.service.AuthService;
import com.tm.tsm_atelier.domain.user.dto.UserResponseDTO;
import com.tm.tsm_atelier.security.RateLimitService;
import com.tm.tsm_atelier.security.SecurityConstants;
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

	@PostMapping("/refresh")
	public ResponseEntity<Object> refresh(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshToken(request);

		if (refreshToken == null) {
			return unauthorized("No refresh token was provided.");
		}

		try {
			AuthResponseDTO newTokens = authService.refresh(refreshToken);
			addCookiesToResponse(response, newTokens);
			return ResponseEntity.ok(newTokens);
		} catch (InvalidTokenException | UserNotFoundException | EmailNotVerifiedException e) {
			clearCookies(response);
			return unauthorized(e.getMessage());
		}
	}

	private ResponseEntity<Object> unauthorized(String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
		problem.setTitle("Invalid token");

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
	}

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
		clearCookies(response);

		return ResponseEntity.ok().build();
	}

	private void addCookiesToResponse(HttpServletResponse response, AuthResponseDTO tokens) {
		ResponseCookie accessCookie = ResponseCookie.from(SecurityConstants.ACCESS_TOKEN_COOKIE, tokens.accessToken())
				.httpOnly(true).secure(true).path("/").maxAge(accessTokenExpiration / 1000)
				.sameSite(SecurityConstants.COOKIE_SAME_SITE).build();

		ResponseCookie refreshCookie = ResponseCookie
				.from(SecurityConstants.REFRESH_TOKEN_COOKIE, tokens.refreshToken()).httpOnly(true).secure(true)
				.path(SecurityConstants.REFRESH_TOKEN_COOKIE_PATH).maxAge(refreshTokenExpiration / 1000)
				.sameSite(SecurityConstants.COOKIE_SAME_SITE).build();

		response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
	}

	private void clearCookies(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie(SecurityConstants.ACCESS_TOKEN_COOKIE, "/"));
		response.addHeader(HttpHeaders.SET_COOKIE,
				expiredCookie(SecurityConstants.REFRESH_TOKEN_COOKIE, SecurityConstants.REFRESH_TOKEN_COOKIE_PATH));
	}

	private String expiredCookie(String name, String path) {
		return ResponseCookie.from(name, "").httpOnly(true).secure(true).path(path).maxAge(0)
				.sameSite(SecurityConstants.COOKIE_SAME_SITE).build().toString();
	}

	private String extractAccessToken(HttpServletRequest request) {
		return readCookie(request, SecurityConstants.ACCESS_TOKEN_COOKIE);
	}

	private String extractRefreshToken(HttpServletRequest request) {
		return readCookie(request, SecurityConstants.REFRESH_TOKEN_COOKIE);
	}

	private String readCookie(HttpServletRequest request, String name) {
		if (request.getCookies() == null) {
			return null;
		}

		for (Cookie cookie : request.getCookies()) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return null;
	}

	private String getClientIp(HttpServletRequest request) {
		return request.getRemoteAddr();
	}
}
