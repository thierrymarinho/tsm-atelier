package com.tm.tsm_atelier.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Marca cada requisição com um id curto e o publica no MDC, para que todas as
 * linhas de log geradas por ela fiquem amarradas umas às outras. Sem isso, num
 * stream único de log, o ERROR do GlobalExceptionHandler não tem como ser
 * ligado à requisição que o causou nem às linhas que vieram antes dele.
 *
 * O id também volta no header da resposta: quando alguém reporta um erro, o
 * valor que o frontend recebeu é o suficiente para achar o rastro completo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID = "requestId";

	private static final String HEADER = "X-Request-Id";
	private static final int MAX_LENGTH = 36;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		String requestId = sanitize(request.getHeader(HEADER));
		if (requestId == null) {
			requestId = UUID.randomUUID().toString().substring(0, 8);
		}

		MDC.put(REQUEST_ID, requestId);
		response.setHeader(HEADER, requestId);

		try {
			chain.doFilter(request, response);
		} finally {
			// A thread volta para o pool do Tomcat no fim da requisição. Deixar o
			// MDC sujo faria a próxima requisição atendida por ela herdar este id.
			MDC.remove(REQUEST_ID);
		}
	}

	/**
	 * O header é enviado pelo cliente e cai direto no arquivo de log. Sem limpeza,
	 * um valor contendo quebra de linha permitiria forjar linhas de log inteiras,
	 * então só sobrevivem caracteres seguros e um tamanho limitado.
	 */
	private String sanitize(String headerValue) {
		if (headerValue == null || headerValue.isBlank()) {
			return null;
		}

		String cleaned = headerValue.trim().replaceAll("[^A-Za-z0-9._-]", "");
		if (cleaned.isEmpty()) {
			return null;
		}

		return cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH) : cleaned;
	}
}
