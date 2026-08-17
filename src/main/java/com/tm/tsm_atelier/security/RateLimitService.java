package com.tm.tsm_atelier.security;

import com.tm.tsm_atelier.common.exception.custom.AccountLockedException;
import com.tm.tsm_atelier.common.exception.custom.TooManyRequestsException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Falha aberto quando o Redis não responde, pela mesma razão do denylist em
 * JwtAuthenticationFilter: aqui o Redis é uma camada de proteção sobre uma
 * autenticação que já funciona sem ela — quem decide se a senha está certa é o
 * Postgres com BCrypt. Perder a trava de conta durante uma queda abre uma
 * janela de força bruta que dura o incidente; falhar fechado derrubaria login,
 * cadastro e reenvio de verificação por inteiro, que é um estrago maior e
 * garantido.
 *
 * AccountLockedException e TooManyRequestsException nascem destes mesmos
 * métodos e precisam continuar subindo, então são lançadas fora do try — nenhum
 * ajuste futuro no catch consegue engoli-las por acidente. O catch segue
 * estreito em DataAccessException mesmo assim: um catch (Exception) esconderia
 * também defeito de programação, e o sintoma seria o silêncio.
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

	private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

	private final StringRedisTemplate redisTemplate;

	public void checkRateLimit(String action, String ip, int maxRequests, Duration duration) {
		String key = "rl:ip:" + action + ":" + ip;
		Long hits;

		try {
			hits = redisTemplate.opsForValue().increment(key);

			if (hits != null && hits == 1L) {
				redisTemplate.expire(key, duration);
			}
		} catch (DataAccessException e) {
			unavailable("IP throttle for action '" + action + "'", e);
			return;
		}

		if (hits != null && hits > maxRequests) {
			if (hits == maxRequests + 1L) {
				log.warn("Rate limit exceeded for action '{}' from IP {}", action, ip);
			}
			throw new TooManyRequestsException("You have exceeded the attempt limit. Please try again later.");
		}
	}

	public void checkAccountLockout(String email) {
		String key = lockoutKey(email);
		long minutes;

		try {
			String value = redisTemplate.opsForValue().get(key);
			if (value == null || Integer.parseInt(value) < 5) {
				return;
			}

			Long expire = redisTemplate.getExpire(key);
			minutes = (expire != null && expire > 0) ? (expire / 60) + 1 : 15;
		} catch (DataAccessException e) {
			unavailable("account lockout check", e);
			return;
		}

		throw new AccountLockedException(
				"Your account has been locked for " + minutes + " minutes due to multiple failed attempts.");
	}

	public void recordFailedAttempt(String email, String ip, int maxAttempts, Duration lockDuration) {
		String key = lockoutKey(email);
		Long attempts;
		boolean locked = false;

		try {
			attempts = redisTemplate.opsForValue().increment(key);

			if (attempts == null) {
				return;
			}

			if (attempts == 1L) {
				redisTemplate.expire(key, Duration.ofHours(1));
			} else if (attempts >= maxAttempts) {
				redisTemplate.expire(key, lockDuration);
				locked = true;
			}
		} catch (DataAccessException e) {
			unavailable("failed attempt bookkeeping", e);
			return;
		}

		if (locked) {
			log.warn("Account lockout triggered for {} from IP {} after {} failed attempts", email, ip, attempts);

			throw new AccountLockedException("Your account has been locked for " + lockDuration.toMinutes()
					+ " minutes due to multiple failed attempts.");
		}
	}

	public void resetFailedAttempts(String email) {
		try {
			redisTemplate.delete(lockoutKey(email));
		} catch (DataAccessException e) {
			unavailable("failed attempt reset", e);
		}
	}

	/**
	 * Em warn, e não em debug: o sistema está rodando com uma garantia a menos, e
	 * isso precisa aparecer em algum lugar.
	 */
	private void unavailable(String guard, DataAccessException e) {
		log.warn("Rate limiting unavailable; proceeding without the {}", guard, e);
	}

	private String lockoutKey(String email) {
		return "rl:lockout:" + email;
	}
}
