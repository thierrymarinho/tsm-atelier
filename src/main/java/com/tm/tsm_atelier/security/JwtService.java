package com.tm.tsm_atelier.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.access-token-expiration}")
	private long jwtExpiration;

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
