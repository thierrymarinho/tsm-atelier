package com.tm.tsm_atelier.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * O logout apagava o refresh token e limpava os cookies, mas o access token
 * seguia válido até expirar. Enquanto ele durava um minuto isso era
 * desprezível; com quinze, uma cópia tirada antes do logout continua abrindo a
 * conta por todo esse tempo — num computador compartilhado, isso importa.
 *
 * <p>
 * O custo é um GET no Redis por requisição autenticada. É aceitável aqui porque
 * o filtro já vai ao banco a cada requisição para carregar as authorities,
 * então a ida ao Redis não introduz uma categoria nova de latência.
 */
@Service
public class AccessTokenDenylist {

	private static final String KEY_PREFIX = "at:revoked:";

	private final StringRedisTemplate redisTemplate;

	public AccessTokenDenylist(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * A entrada vive só até o instante em que o token expiraria por conta própria.
	 * Depois disso ela seria peso morto: o token já não passa na validação.
	 */
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

	/**
	 * O token nunca é guardado em claro, mesmo revogado: um dump do Redis não pode
	 * devolver credenciais utilizáveis. Mesma razão do hash dos refresh tokens.
	 */
	private String key(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return KEY_PREFIX + HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is required to store revoked access tokens", e);
		}
	}
}
