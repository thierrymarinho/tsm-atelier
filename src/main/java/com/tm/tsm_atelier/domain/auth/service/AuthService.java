package com.tm.tsm_atelier.domain.auth.service;

import com.tm.tsm_atelier.common.exception.custom.EmailAlreadyExistsException;
import com.tm.tsm_atelier.common.exception.custom.EmailAlreadyVerifiedException;
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
import com.tm.tsm_atelier.security.RateLimitService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final StringRedisTemplate redisTemplate;
	private final EmailPort emailPort;
	private final RateLimitService rateLimitService;

	@Value("${jwt.refresh-token-expiration}")
	private long refreshTokenExpiration;

	@Value("${app.email-verification-expiration}")
	private long emailVerificationExpiration;

	@Value("${app.base-url}")
	private String appBaseUrl;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			AuthenticationManager authenticationManager, StringRedisTemplate redisTemplate, EmailPort emailPort,
			RateLimitService rateLimitService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
		this.redisTemplate = redisTemplate;
		this.emailPort = emailPort;
		this.rateLimitService = rateLimitService;
	}

	@Transactional
	public RegisterResponseDTO register(RegisterRequestDTO request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new EmailAlreadyExistsException("Email is already in use.");
		}

		User user = User.builder().firstName(request.firstName()).lastName(request.lastName()).email(request.email())
				.password(passwordEncoder.encode(request.password())).role(Role.CUSTOMER).emailVerified(false).build();

		userRepository.save(user);

		// Gera token de verificação e persiste no Redis com TTL de 24h
		String verificationToken = UUID.randomUUID().toString();
		redisTemplate.opsForValue().set("emailVerification:" + verificationToken, request.email(),
				Duration.ofMillis(emailVerificationExpiration));

		// Monta o link que o frontend irá consumir para chamar a API
		String verificationLink = appBaseUrl + "/verify-email?token=" + verificationToken;

		// Disparo assíncrono — não bloqueia o retorno do register
		emailPort.sendVerificationEmail(request.email(), request.firstName(), verificationLink);

		return new RegisterResponseDTO("Registration successful. Please check your email to verify your account.");
	}

	@Transactional(readOnly = true)
	public AuthResponseDTO login(LoginRequestDTO request, String clientIp) {
		rateLimitService.checkAccountLockout(request.email(), clientIp);

		try {
			authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
		} catch (BadCredentialsException e) {
			rateLimitService.recordFailedAttempt(request.email(), clientIp, 5, Duration.ofMinutes(15));
			throw e;
		}

		rateLimitService.resetFailedAttempts(request.email(), clientIp);

		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new UserNotFoundException("User not found."));

		if (!user.isEmailVerified()) {
			throw new EmailNotVerifiedException("Please verify your email before logging in.");
		}

		return generateAndSaveTokens(user);
	}

	@Transactional
	public AuthResponseDTO verifyEmail(String token) {
		String email = redisTemplate.opsForValue().get("emailVerification:" + token);

		if (email == null) {
			throw new InvalidTokenException("Invalid or expired verification token.");
		}

		User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found."));

		user.setEmailVerified(true);
		userRepository.save(user);

		// Remove o token de verificação após uso
		redisTemplate.delete("emailVerification:" + token);

		return generateAndSaveTokens(user);
	}

	public void resendVerificationEmail(String email) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found."));

		if (user.isEmailVerified()) {
			throw new EmailAlreadyVerifiedException("This email is already verified.");
		}

		String verificationToken = UUID.randomUUID().toString();
		redisTemplate.opsForValue().set("emailVerification:" + verificationToken, email,
				Duration.ofMillis(emailVerificationExpiration));

		String verificationLink = appBaseUrl + "/verify-email?token=" + verificationToken;
		emailPort.sendVerificationEmail(email, user.getFirstName(), verificationLink);
	}

	public AuthResponseDTO refresh(String refreshToken) {
		String hashedToken = hashToken(refreshToken);
		String email = redisTemplate.opsForValue().get("rt:valid:" + hashedToken);

		if (email == null) {
			String reusedEmail = redisTemplate.opsForValue().get("rt:used:" + hashedToken);
			if (reusedEmail != null) {
				revokeAllUserTokens(reusedEmail);
				throw new InvalidTokenException(
						"Security Alert: Token reuse detected. All sessions have been revoked.");
			}
			throw new InvalidTokenException("Invalid or expired refresh token.");
		}

		User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found."));

		// Invalidate old token (Rotation)
		redisTemplate.delete("rt:valid:" + hashedToken);
		redisTemplate.opsForSet().remove("rt:user:" + email, hashedToken);

		// Mark as used to detect future replay attacks
		redisTemplate.opsForValue().set("rt:used:" + hashedToken, email, Duration.ofMillis(refreshTokenExpiration));

		return generateAndSaveTokens(user);
	}

	@Transactional(readOnly = true)
	public UserResponseDTO getMe(String email) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found."));

		String fullName = formatFullName(user.getFirstName(), user.getLastName());
		return new UserResponseDTO(user.getId(), user.getFirstName(), user.getLastName(), fullName, user.getEmail(),
				user.getRole());
	}

	public void logout(String refreshToken) {
		if (refreshToken != null) {
			String hashedToken = hashToken(refreshToken);
			String email = redisTemplate.opsForValue().get("rt:valid:" + hashedToken);
			if (email != null) {
				redisTemplate.delete("rt:valid:" + hashedToken);
				redisTemplate.opsForSet().remove("rt:user:" + email, hashedToken);
			}
		}
	}

	private AuthResponseDTO generateAndSaveTokens(User user) {
		String accessToken = jwtService.generateToken(user);
		String rawRefreshToken = UUID.randomUUID().toString();
		String hashedToken = hashToken(rawRefreshToken);

		redisTemplate.opsForValue().set("rt:valid:" + hashedToken, user.getEmail(),
				Duration.ofMillis(refreshTokenExpiration));

		redisTemplate.opsForSet().add("rt:user:" + user.getEmail(), hashedToken);

		String fullName = formatFullName(user.getFirstName(), user.getLastName());
		return new AuthResponseDTO(accessToken, rawRefreshToken, user.getEmail(), fullName);
	}

	private String formatFullName(String firstName, String lastName) {
		if (firstName == null && lastName == null) {
			return "";
		}
		if (firstName == null) {
			return lastName.trim();
		}
		if (lastName == null) {
			return firstName.trim();
		}
		return (firstName.trim() + " " + lastName.trim()).trim();
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Error hashing token", e);
		}
	}

	private void revokeAllUserTokens(String email) {
		Set<String> activeHashes = redisTemplate.opsForSet().members("rt:user:" + email);
		if (activeHashes != null && !activeHashes.isEmpty()) {
			for (String hash : activeHashes) {
				redisTemplate.delete("rt:valid:" + hash);
			}
		}
		redisTemplate.delete("rt:user:" + email);
	}
}
