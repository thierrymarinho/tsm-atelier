package com.tm.tsm_atelier.security;

public final class SecurityConstants {

	private SecurityConstants() {
	}

	public static final String[] PUBLIC_ROUTES = {"/api/v1/auth/**", "/api/v1/catalog/**", "/v3/api-docs/**",
			"/swagger-ui/**", "/swagger-ui.html", "/api/v1/webhooks/**"};
}
