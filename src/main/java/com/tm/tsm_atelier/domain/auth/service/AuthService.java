package com.tm.tsm_atelier.domain.auth.service;

import com.tm.tsm_atelier.common.exception.custom.EmailAlreadyExistsException;
import com.tm.tsm_atelier.common.exception.custom.EmailNotVerifiedException;
import com.tm.tsm_atelier.common.exception.custom.InvalidTokenException;
import com.tm.tsm_atelier.common.exception.custom.UserNotFoundException;
import com.tm.tsm_atelier.common.service.EmailService;
import com.tm.tsm_atelier.domain.auth.dto.AuthResponseDTO;
import com.tm.tsm_atelier.domain.auth.dto.LoginRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterRequestDTO;
import com.tm.tsm_atelier.domain.auth.dto.RegisterResponseDTO;
import com.tm.tsm_atelier.domain.user.dto.UserResponseDTO;
import com.tm.tsm_atelier.domain.user.entity.Role;
import com.tm.tsm_atelier.domain.user.entity.User;
import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import com.tm.tsm_atelier.security.JwtService;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
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
	private final EmailService emailService;

	@Value("${jwt.refresh-token-expiration}")
	private long refreshTokenExpiration;

	@Value("${app.email-verification-expiration}")
	private long emailVerificationExpiration;

	@Value("${app.base-url}")
	private String appBaseUrl;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			AuthenticationManager authenticationManager, StringRedisTemplate redisTemplate, EmailService emailService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
		this.redisTemplate = redisTemplate;
		this.emailService = emailService;
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
		emailService.sendVerificationEmail(request.email(), request.firstName(), verificationLink);

		return new RegisterResponseDTO("Registration successful. Please check your email to verify your account.");
	}

	@Transactional(readOnly = true)
	public AuthResponseDTO login(LoginRequestDTO request) {
		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

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

	public AuthResponseDTO refresh(String refreshToken) {
		String email = redisTemplate.opsForValue().get("refreshToken:" + refreshToken);

		if (email == null) {
			throw new InvalidTokenException("Invalid or expired refresh token.");
		}

		User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found."));

		redisTemplate.delete("refreshToken:" + refreshToken);

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
			redisTemplate.delete("refreshToken:" + refreshToken);
		}
	}

	private AuthResponseDTO generateAndSaveTokens(User user) {
		String accessToken = jwtService.generateToken(user);
		String refreshToken = UUID.randomUUID().toString();

		redisTemplate.opsForValue().set("refreshToken:" + refreshToken, user.getEmail(),
				Duration.ofMillis(refreshTokenExpiration));

		String fullName = formatFullName(user.getFirstName(), user.getLastName());
		return new AuthResponseDTO(accessToken, refreshToken, user.getEmail(), fullName);
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
}
