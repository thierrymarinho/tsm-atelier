package com.tm.tsm_atelier.domain.auth.controller.v1;

import com.tm.tsm_atelier.common.exception.custom.InvalidTokenException;
import com.tm.tsm_atelier.common.exception.custom.UserNotFoundException;
import com.tm.tsm_atelier.domain.auth.dto.AuthResponseDTO;
import com.tm.tsm_atelier.domain.auth.dto.LoginRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterResponseDTO;
import com.tm.tsm_atelier.domain.auth.dto.ResendVerificationRequestDTO;
import com.tm.tsm_atelier.domain.auth.service.AuthService;
import com.tm.tsm_atelier.domain.user.dto.UserResponseDTO;
import com.tm.tsm_atelier.security.RateLimitService;
import com.tm.tsm_atelier.security.JwtService;
import java.time.Duration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final JwtService jwtService;
	private final RateLimitService rateLimitService;

	@Value("${jwt.access-token-expiration}")
	private long accessTokenExpiration;

	@Value("${jwt.refresh-token-expiration}")
	private long refreshTokenExpiration;

	public AuthController(AuthService authService, JwtService jwtService, RateLimitService rateLimitService) {
		this.authService = authService;
		this.jwtService = jwtService;
		this.rateLimitService = rateLimitService;
	}

	@PostMapping("/register")
	public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request, HttpServletRequest httpRequest) {
		rateLimitService.checkRateLimit("register", getClientIp(httpRequest), 5, Duration.ofMinutes(15));
		RegisterResponseDTO response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/verify-email")
	public ResponseEntity<Void> verifyEmail(@RequestParam String token, HttpServletResponse response) {
		AuthResponseDTO tokens = authService.verifyEmail(token);
		addCookiesToResponse(response, tokens);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/resend-verification")
	public ResponseEntity<Void> resendVerificationEmail(@Valid @RequestBody ResendVerificationRequestDTO request, HttpServletRequest httpRequest) {
		rateLimitService.checkRateLimit("resend-verification", getClientIp(httpRequest), 3, Duration.ofMinutes(15));
		authService.resendVerificationEmail(request.email());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request,
			HttpServletRequest httpRequest, HttpServletResponse response) {
		rateLimitService.checkRateLimit("login", getClientIp(httpRequest), 10, Duration.ofMinutes(15));
		AuthResponseDTO tokens = authService.login(request);
		addCookiesToResponse(response, tokens);
		return ResponseEntity.ok(tokens);
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponseDTO> refresh(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshToken(request);
		if (refreshToken == null) {
			return ResponseEntity.status(401).build();
		}

		try {
			AuthResponseDTO newTokens = authService.refresh(refreshToken);
			addCookiesToResponse(response, newTokens);
			return ResponseEntity.ok(newTokens);
		} catch (InvalidTokenException | UserNotFoundException e) {
			return ResponseEntity.status(401).build();
		}
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponseDTO> getMe(HttpServletRequest request) {
		String email = null;

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()
				&& !"anonymousUser".equals(authentication.getPrincipal())) {
			email = authentication.getName();
		}

		if (email == null) {
			String jwt = extractAccessToken(request);
			if (jwt != null) {
				try {
					String extractedEmail = jwtService.extractUsername(jwt);
					if (extractedEmail != null && jwtService.isTokenValid(jwt, extractedEmail)) {
						email = extractedEmail;
					}
				} catch (Exception ignored) {
				}
			}
		}

		if (email == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		try {
			UserResponseDTO userProfile = authService.getMe(email);
			return ResponseEntity.ok(userProfile);
		} catch (UserNotFoundException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractRefreshToken(request);
		authService.logout(refreshToken);

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

	private String extractAccessToken(HttpServletRequest request) {
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if ("access_token".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
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

	private String getClientIp(HttpServletRequest request) {
		String ipAddress = request.getHeader("X-Forwarded-For");
		if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getRemoteAddr();
		}
		if (ipAddress != null && ipAddress.contains(",")) {
			ipAddress = ipAddress.split(",")[0].trim();
		}
		return ipAddress;
	}
}
