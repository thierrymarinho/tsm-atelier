package com.tm.tsm_atelier.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	/** HS256 exige 256 bits. Abaixo disso a chave é fraca por definição. */
	private static final int MIN_SECRET_BYTES = 32;

	/**
	 * Valores que já circularam publicamente e não podem voltar a assinar nada.
	 * Este estava versionado no application.yaml e continua no histórico do git —
	 * trocar a variável de ambiente não o apaga de lá, então ele fica barrado para
	 * sempre.
	 */
	private static final Set<String> COMPROMISED_SECRETS = Set.of("super-secret-key-for-jwt-generation-tsm-atelier");

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.access-token-expiration}")
	private long jwtExpiration;

	/**
	 * Validado na subida, e não no primeiro uso: uma chave fraca só estouraria na
	 * primeira emissão de token, ou seja, em produção, no primeiro login. Falhar
	 * aqui transforma isso em erro de deploy.
	 */
	@PostConstruct
	void validateSecret() {
		if (secretKey == null || secretKey.isBlank()) {
			throw new IllegalStateException("JWT_SECRET is required and must not be blank.");
		}

		if (COMPROMISED_SECRETS.contains(secretKey)) {
			throw new IllegalStateException(
					"JWT_SECRET is set to a value published in this repository's history. Rotate it.");
		}

		if (secretKey.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes long for HS256.");
		}
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	public String generateToken(UserDetails userDetails) {
		return buildToken(userDetails, jwtExpiration);
	}

	public String extractRole(String token) {
		return extractClaim(token, claims -> claims.get("role", String.class));
	}

	private String buildToken(UserDetails userDetails, long expiration) {
		String role = userDetails.getAuthorities().stream().findFirst().map(a -> a.getAuthority())
				.orElse("ROLE_CUSTOMER");

		return Jwts.builder().subject(userDetails.getUsername()).claim("role", role)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + expiration)).signWith(getSignInKey(), Jwts.SIG.HS256)
				.compact();
	}

	/**
	 * Quanto ainda falta para o token expirar sozinho. O denylist de logout usa
	 * isso como TTL, para a entrada não sobreviver ao token que ela revoga.
	 */
	public Duration remainingValidity(String token) {
		long remaining = extractExpiration(token).getTime() - System.currentTimeMillis();
		return remaining > 0 ? Duration.ofMillis(remaining) : Duration.ZERO;
	}

	public boolean isTokenValid(String token, String expectedUsername) {
		final String username = extractUsername(token);
		return (username.equals(expectedUsername)) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
	}

	private SecretKey getSignInKey() {
		return io.jsonwebtoken.security.Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	}
}
