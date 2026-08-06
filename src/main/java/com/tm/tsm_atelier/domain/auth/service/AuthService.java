package com.tm.tsm_atelier.domain.auth.service;

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
import com.tm.tsm_atelier.security.AccessTokenDenylist;
import com.tm.tsm_atelier.security.JwtService;
import com.tm.tsm_atelier.security.RateLimitService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final StringRedisTemplate redisTemplate;
	private final EmailPort emailPort;
	private final RateLimitService rateLimitService;
	private final AccessTokenDenylist accessTokenDenylist;

	@Value("${jwt.refresh-token-expiration}")
	private long refreshTokenExpiration;

	@Value("${app.email-verification-expiration}")
	private long emailVerificationExpiration;

	/**
	 * Por quanto tempo um refresh token recem-rotacionado ainda e aceito. Existe
	 * porque o cliente legitimo reaparece com o token antigo o tempo todo: resposta
	 * perdida no caminho, timeout, duas abas renovando juntas. Sem a janela, cada
	 * um desses casos caia na deteccao de reuso e derrubava todas as sessoes do
	 * usuario. O preco e que um replay dentro da janela passa despercebido, entao
	 * ela e curta de proposito.
	 */
	@Value("${app.refresh-token-grace-ms:30000}")
	private long refreshTokenGraceMs;

	@Value("${app.base-url}")
	private String appBaseUrl;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			AuthenticationManager authenticationManager, StringRedisTemplate redisTemplate, EmailPort emailPort,
			RateLimitService rateLimitService, AccessTokenDenylist accessTokenDenylist) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
		this.redisTemplate = redisTemplate;
		this.emailPort = emailPort;
		this.rateLimitService = rateLimitService;
		this.accessTokenDenylist = accessTokenDenylist;
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

		redisTemplate.delete("emailVerification:" + token);

		return generateAndSaveTokens(user);
	}

	/**
	 * Não sinaliza se a conta existe nem se já foi verificada. Antes, um e-mail
	 * desconhecido voltava 400 "User not found" e um já verificado voltava outro
	 * erro — bastava iterar uma lista para descobrir quem tem conta na loja, que é
	 * insumo direto para phishing dirigido. O rate limit por IP limitava a
	 * velocidade, não o vazamento. A rota agora responde igual nos três casos e só
	 * envia o e-mail quando faz sentido enviar.
	 */
	public void resendVerificationEmail(String email) {
		User user = userRepository.findByEmail(email).orElse(null);

		if (user == null || user.isEmailVerified()) {
			return;
		}

		String verificationToken = UUID.randomUUID().toString();
		redisTemplate.opsForValue().set("emailVerification:" + verificationToken, email,
				Duration.ofMillis(emailVerificationExpiration));

		String verificationLink = appBaseUrl + "/verify-email?token=" + verificationToken;
		emailPort.sendVerificationEmail(email, user.getFirstName(), verificationLink);
	}

	public AuthResponseDTO refresh(String refreshToken) {
		String hashedToken = hashToken(refreshToken);

		// GETDEL em vez de GET seguido de DELETE: com as duas operacoes separadas,
		// duas requisicoes simultaneas com o mesmo token liam a chave antes de
		// qualquer uma apagar e as duas eram atendidas — sem disparar a deteccao de
		// reuso, que e exatamente o cenario que ela existe para pegar. Agora so uma
		// consegue consumir o token.
		String email = redisTemplate.opsForValue().getAndDelete("rt:valid:" + hashedToken);

		if (email != null) {
			redisTemplate.opsForSet().remove("rt:user:" + email, hashedToken);

			// Marcado antes de ir ao banco para estreitar ao maximo o intervalo em que
			// uma requisicao concorrente veria o token como simplesmente inexistente.
			if (refreshTokenGraceMs > 0) {
				redisTemplate.opsForValue().set("rt:grace:" + hashedToken, email,
						Duration.ofMillis(refreshTokenGraceMs));
			}
			redisTemplate.opsForValue().set("rt:used:" + hashedToken, email, Duration.ofMillis(refreshTokenExpiration));

			return issueTokensFor(email);
		}

		// Retry do cliente dentro da janela: trata como renovacao normal, nao como
		// ataque. E checado antes de rt:used justamente para nao revogar tudo.
		String graceEmail = redisTemplate.opsForValue().get("rt:grace:" + hashedToken);
		if (graceEmail != null) {
			return issueTokensFor(graceEmail);
		}

		String reusedEmail = redisTemplate.opsForValue().get("rt:used:" + hashedToken);
		if (reusedEmail != null) {
			// Único sinal de que um refresh token vazou, e ele não deixa rastro no
			// banco. O usuário também não reclama: ele só é deslogado. Sem esta
			// linha, a revogação em massa acontece sem nenhuma evidência de por quê.
			// O e-mail entra aqui de propósito — é um evento de segurança, e sem ele
			// não há como saber qual conta investigar.
			log.warn("Refresh token reuse detected for {}; revoking all sessions", reusedEmail);
			revokeAllUserTokens(reusedEmail);
			throw new InvalidTokenException("Security Alert: Token reuse detected. All sessions have been revoked.");
		}

		throw new InvalidTokenException("Invalid or expired refresh token.");
	}

	/**
	 * O refresh repete as validacoes de acesso do login. Antes so o login barrava
	 * conta nao verificada, entao quem ja tivesse uma sessao aberta seguia
	 * renovando indefinidamente mesmo depois de perder a verificacao.
	 */
	private AuthResponseDTO issueTokensFor(String email) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found."));

		if (!user.isEmailVerified()) {
			throw new EmailNotVerifiedException("Please verify your email before logging in.");
		}

		return generateAndSaveTokens(user);
	}

	@Transactional(readOnly = true)
	public UserResponseDTO getMe(String email) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found."));

		String fullName = formatFullName(user.getFirstName(), user.getLastName());
		return new UserResponseDTO(user.getId(), user.getFirstName(), user.getLastName(), fullName, user.getEmail(),
				user.getRole());
	}

	public void logout(String accessToken, String refreshToken) {
		// O access token sobrevive ao logout até expirar: assinatura válida, dentro
		// do prazo. Com 15 minutos de vida, uma cópia tirada antes do logout seguia
		// abrindo a conta por todo esse tempo. O denylist encerra isso agora.
		if (accessToken != null) {
			try {
				accessTokenDenylist.revoke(accessToken, jwtService.remainingValidity(accessToken));
			} catch (Exception e) {
				// Token ilegível ou já expirado — não há o que revogar, e um logout
				// nunca deve falhar por causa da credencial que está descartando.
				log.debug("Could not revoke the access token during logout: {}", e.getMessage());
			}
		}

		if (refreshToken == null) {
			return;
		}

		String hashedToken = hashToken(refreshToken);
		String email = redisTemplate.opsForValue().getAndDelete("rt:valid:" + hashedToken);

		// Um logout dentro da janela de graca precisa mata-la tambem, senao o token
		// que acabou de ser invalidado ainda renderia uma sessao nova.
		redisTemplate.delete("rt:grace:" + hashedToken);

		if (email != null) {
			redisTemplate.opsForSet().remove("rt:user:" + email, hashedToken);
		}
	}

	private AuthResponseDTO generateAndSaveTokens(User user) {
		String accessToken = jwtService.generateToken(user);
		String rawRefreshToken = UUID.randomUUID().toString();
		String hashedToken = hashToken(rawRefreshToken);

		redisTemplate.opsForValue().set("rt:valid:" + hashedToken, user.getEmail(),
				Duration.ofMillis(refreshTokenExpiration));

		redisTemplate.opsForSet().add("rt:user:" + user.getEmail(), hashedToken);

		// O set nunca expirava: hashes de tokens que morreram por TTL ficavam nele
		// para sempre, porque so a rotacao e o logout removem membros. Renovar o
		// prazo a cada emissao faz o indice morrer junto com o ultimo token que ele
		// guarda.
		redisTemplate.expire("rt:user:" + user.getEmail(), Duration.ofMillis(refreshTokenExpiration));

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
				redisTemplate.delete("rt:grace:" + hash);
			}
		}
		redisTemplate.delete("rt:user:" + email);
	}
}
