package com.tm.tsm_atelier.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.tm.tsm_atelier.common.exception.custom.AccountLockedException;
import com.tm.tsm_atelier.common.exception.custom.TooManyRequestsException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * O Redis aqui protege o login, mas não o realiza — a senha é decidida no
 * Postgres. Estes testes fixam as duas metades dessa decisão: uma queda do
 * Redis não pode derrubar autenticação, e a proteção não pode sumir quando o
 * Redis está de pé.
 *
 * A segunda metade existe porque a primeira é fácil de exagerar: quem alargar o
 * catch ou movê-lo para envolver o throw desliga a proteção sem quebrar nada
 * visível — nenhum outro teste do projeto exercita este serviço.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService")
class RateLimitServiceTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private RateLimitService rateLimitService;

	private void redisIsDown() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	@Nested
	@DisplayName("com o Redis fora")
	class RedisUnavailable {

		private final RedisConnectionFailureException failure = new RedisConnectionFailureException("Redis fora");

		@Test
		@DisplayName("o throttle por IP libera a requisição")
		void ipThrottleFailsOpen() {
			redisIsDown();
			when(valueOperations.increment(anyString())).thenThrow(failure);

			assertThatCode(() -> rateLimitService.checkRateLimit("login", "1.2.3.4", 10, Duration.ofMinutes(15)))
					.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("a trava de conta libera o login")
		void accountLockoutFailsOpen() {
			redisIsDown();
			when(valueOperations.get(anyString())).thenThrow(failure);

			assertThatCode(() -> rateLimitService.checkAccountLockout("a@b.com")).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("a contagem de tentativa falha não propaga o erro")
		void recordingFailsSilently() {
			redisIsDown();
			when(valueOperations.increment(anyString())).thenThrow(failure);

			assertThatCode(() -> rateLimitService.recordFailedAttempt("a@b.com", "1.2.3.4", 5, Duration.ofMinutes(15)))
					.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("o reset após login bem-sucedido não propaga o erro")
		void resetFailsSilently() {
			when(redisTemplate.delete(anyString())).thenThrow(failure);

			assertThatCode(() -> rateLimitService.resetFailedAttempts("a@b.com")).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("com o Redis de pé")
	class RedisAvailable {

		@Test
		@DisplayName("o throttle por IP continua barrando acima do limite")
		void ipThrottleStillBlocks() {
			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.increment(anyString())).thenReturn(11L);

			assertThatThrownBy(() -> rateLimitService.checkRateLimit("login", "1.2.3.4", 10, Duration.ofMinutes(15)))
					.isInstanceOf(TooManyRequestsException.class);
		}

		@Test
		@DisplayName("a trava de conta continua barrando o login")
		void accountLockoutStillBlocks() {
			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.get(anyString())).thenReturn("5");
			when(redisTemplate.getExpire(anyString())).thenReturn(600L);

			assertThatThrownBy(() -> rateLimitService.checkAccountLockout("a@b.com"))
					.isInstanceOf(AccountLockedException.class).hasMessageContaining("11 minutes");
		}

		@Test
		@DisplayName("a tentativa que estoura o limite continua travando a conta")
		void lastAttemptStillLocks() {
			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.increment(anyString())).thenReturn(5L);

			assertThatThrownBy(
					() -> rateLimitService.recordFailedAttempt("a@b.com", "1.2.3.4", 5, Duration.ofMinutes(15)))
					.isInstanceOf(AccountLockedException.class);
		}
	}
}
