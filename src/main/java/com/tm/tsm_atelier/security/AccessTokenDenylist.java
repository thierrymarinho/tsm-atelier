package com.tm.tsm_atelier.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenDenylist {

	private static final String KEY_PREFIX = "at:revoked:";

	private final StringRedisTemplate redisTemplate;

	public AccessTokenDenylist(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void revoke(String token, Duration remainingValidity) {
		if (token == null || remainingValidity == null || remainingValidity.isZero()
				|| remainingValidity.isNegative()) {
			return;
		}

		redisTemplate.opsForValue().set(key(token), "1", remainingValidity);
	}

	public boolean isRevoked(String token) {
		return token != null && Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
	}

	private String key(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return KEY_PREFIX + HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is required to store revoked access tokens", e);
		}
	}
}
