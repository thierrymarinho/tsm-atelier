package com.tm.tsm_atelier.security;

import java.util.stream.Stream;

public final class SecurityConstants {

	private SecurityConstants() {
	}

	/**
	 * Rotas de autenticação anteriores à sessão: não há credencial a proteger nem
	 * ação a forjar, então dispensam CSRF. A listagem é explícita porque o curinga
	 * /api/v1/auth/** que existia aqui arrastava junto /me e /logout — e era por
	 * causa de /me público que o controller validava o JWT na mão, mantendo um
	 * segundo caminho de autenticação em paralelo ao filtro.
	 */
	public static final String[] PRE_SESSION_AUTH_ROUTES = {"/api/v1/auth/login", "/api/v1/auth/register",
			"/api/v1/auth/verify-email", "/api/v1/auth/resend-verification", "/api/v1/auth/refresh"};

	/**
	 * O Stripe não tem como enviar um token CSRF; a barreira dele é a assinatura
	 * HMAC do payload, verificada no controller.
	 */
	public static final String[] CSRF_EXEMPT_ROUTES = Stream
			.concat(Stream.of("/api/v1/webhooks/stripe"), Stream.of(PRE_SESSION_AUTH_ROUTES)).toArray(String[]::new);

	/**
	 * /logout entra aqui, mas não na isenção de CSRF: ele descarta credenciais, e
	 * exigir sessão válida para sair travaria justamente quem está com o access
	 * token expirado — mas um site hostil também não deveria conseguir deslogar
	 * ninguém.
	 */
	public static final String[] PUBLIC_ROUTES = Stream
			.concat(Stream.of(PRE_SESSION_AUTH_ROUTES), Stream.of("/api/v1/auth/logout", "/api/v1/catalog/**",
					"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/v1/webhooks/**"))
			.toArray(String[]::new);
}
