package com.tm.tsm_atelier.security;

import com.tm.tsm_atelier.common.exception.custom.AccountLockedException;
import com.tm.tsm_atelier.common.exception.custom.TooManyRequestsException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {

	private final StringRedisTemplate redisTemplate;

	/**
	 * Valida se um determinado IP não excedeu o limite de requisições na janela de
	 * tempo.
	 * 
	 * @param action
	 *            O nome da ação (ex: "login", "register")
	 * @param ip
	 *            O IP do cliente
	 * @param maxRequests
	 *            O limite máximo permitido na janela
	 * @param duration
	 *            A duração da janela de tempo
	 */
	public void checkRateLimit(String action, String ip, int maxRequests, Duration duration) {
		String key = "rl:ip:" + action + ":" + ip;
		Long hits = redisTemplate.opsForValue().increment(key);

		if (hits != null && hits == 1L) {
			redisTemplate.expire(key, duration);
		}

		if (hits != null && hits > maxRequests) {
			throw new TooManyRequestsException("Você excedeu o limite de tentativas. Tente novamente mais tarde.");
		}
	}

	/**
	 * Verifica se a conta está bloqueada (Lockout). Se não, lança a exceção padrão.
	 *
	 * @param email
	 *            O email do usuário
	 * @param maxAttempts
	 *            Máximo de tentativas falhas consecutivas permitidas
	 * @param lockDuration
	 *            O tempo de bloqueio após o limite ser atingido
	 */
	public void checkAccountLockout(String email) {
		String key = "rl:lockout:" + email;
		String value = redisTemplate.opsForValue().get(key);
		if (value != null && Integer.parseInt(value) >= 5) {
			// A regra de negócio é fixa em 5 tentativas e 15 minutos, conforme acordo com o
			// cliente.
			// O ttl poderia ser lido do Redis, mas por simplicidade lançamos a exceção.
			Long expire = redisTemplate.getExpire(key);
			long minutes = (expire != null && expire > 0) ? (expire / 60) + 1 : 15;
			throw new AccountLockedException(
					"Sua conta foi bloqueada por " + minutes + " minutos devido a múltiplas falhas.");
		}
	}

	/**
	 * Registra uma tentativa falha de login para a conta. Se atingir o limite,
	 * renova a janela de tempo como "Lock".
	 *
	 * @param email
	 *            O email do usuário
	 * @param maxAttempts
	 *            Máximo de tentativas falhas consecutivas permitidas
	 * @param lockDuration
	 *            O tempo de bloqueio após o limite ser atingido
	 */
	public void recordFailedAttempt(String email, int maxAttempts, Duration lockDuration) {
		String key = "rl:lockout:" + email;
		Long attempts = redisTemplate.opsForValue().increment(key);

		if (attempts != null) {
			if (attempts == 1L) {
				// Primeira falha: damos um tempo (ex: 1 hora) para as tentativas acumularem
				redisTemplate.expire(key, Duration.ofHours(1));
			} else if (attempts >= maxAttempts) {
				// Se atingiu o limite (ex: 5ª falha), "Trava" a conta reiniciando o tempo para
				// a duração de Lockout
				redisTemplate.expire(key, lockDuration);
				throw new AccountLockedException("Sua conta foi bloqueada por " + lockDuration.toMinutes()
						+ " minutos devido a múltiplas falhas.");
			}
		}
	}

	/**
	 * Reseta o contador de falhas após um login bem sucedido.
	 *
	 * @param email
	 *            O email do usuário
	 */
	public void resetFailedAttempts(String email) {
		String key = "rl:lockout:" + email;
		redisTemplate.delete(key);
	}
}
