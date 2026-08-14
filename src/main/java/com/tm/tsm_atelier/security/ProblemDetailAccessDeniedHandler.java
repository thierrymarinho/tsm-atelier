package com.tm.tsm_atelier.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Faz o 403 nascido na cadeia de segurança sair como ProblemDetail, igual ao
 * resto da API.
 *
 * O handler padrão do Spring Security não passa pelo RestControllerAdvice: a
 * negação acontece antes de existir um controller, e a resposta saía com o
 * envelope do Boot — timestamp, error, path — enquanto todo o resto da API usa
 * detail, instance, title. Um cliente que lê "detail" recebia undefined em todo
 * 403 de CSRF ausente, que é o caso mais comum deles.
 *
 * O corpo é escrito direto no response, e isso é deliberado: chamar sendError
 * faria o Tomcat despachar para /error, e esse segundo despacho atravessa a
 * cadeia de segurança de novo com o contexto já limpo — foi assim que todo 403
 * desta API já chegou ao cliente como 401.
 */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {

		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		String detail = escape(accessDeniedException.getMessage());
		String instance = escape(request.getRequestURI());

		response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Access denied\",\"status\":403,"
				+ "\"detail\":\"" + detail + "\",\"instance\":\"" + instance + "\"}");
	}

	private String escape(String value) {
		if (value == null) {
			return "Access denied";
		}
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
	}
}
