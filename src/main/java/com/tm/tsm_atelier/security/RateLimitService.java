package com.tm.tsm_atelier.security;

import com.tm.tsm_atelier.common.exception.custom.AccountLockedException;
import com.tm.tsm_atelier.common.exception.custom.TooManyRequestsException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {

	private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

	private final StringRedisTemplate redisTemplate;

	public void checkRateLimit(String action, String ip, int maxRequests, Duration duration) {
		String key = "rl:ip:" + action + ":" + ip;
		Long hits = redisTemplate.opsForValue().increment(key);

		if (hits != null && hits == 1L) {
			redisTemplate.expire(key, duration);
		}

		if (hits != null && hits > maxRequests) {
			throw new TooManyRequestsException("You have exceeded the attempt limit. Please try again later.");
		}
	}

	public void checkAccountLockout(String email, String ip) {
		String key = lockoutKey(email, ip);
		String value = redisTemplate.opsForValue().get(key);
		if (value != null && Integer.parseInt(value) >= 5) {
			Long expire = redisTemplate.getExpire(key);
			long minutes = (expire != null && expire > 0) ? (expire / 60) + 1 : 15;
			throw new AccountLockedException(
					"Your account has been locked for " + minutes + " minutes due to multiple failed attempts.");
		}
	}

	public void recordFailedAttempt(String email, String ip, int maxAttempts, Duration lockDuration) {
		String key = lockoutKey(email, ip);
		Long attempts = redisTemplate.opsForValue().increment(key);

		if (attempts != null) {
			if (attempts == 1L) {
				redisTemplate.expire(key, Duration.ofHours(1));
			} else if (attempts >= maxAttempts) {
				redisTemplate.expire(key, lockDuration);
				throw new AccountLockedException("Your account has been locked for " + lockDuration.toMinutes()
						+ " minutes due to multiple failed attempts.");
			}
		}
	}

	public void resetFailedAttempts(String email, String ip) {
		String key = lockoutKey(email, ip);
		redisTemplate.delete(key);
	}

	private String lockoutKey(String email, String ip) {
		return "rl:lockout:" + email + ":" + (ip == null ? "unknown" : ip);
	}
}
