package com.tm.tsm_atelier.security;

import java.util.stream.Stream;

public final class SecurityConstants {

	private SecurityConstants() {
	}

	public static final String ACCESS_TOKEN_COOKIE = "__Host-access_token";

	public static final String CSRF_COOKIE = "__Host-XSRF-TOKEN";

	public static final String REFRESH_TOKEN_COOKIE = "__Secure-refresh_token";

	public static final String REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth";

	public static final String COOKIE_SAME_SITE = "Strict";

	public static final String[] PRE_SESSION_AUTH_ROUTES = {"/api/v1/auth/login", "/api/v1/auth/register",
			"/api/v1/auth/verify-email", "/api/v1/auth/resend-verification", "/api/v1/auth/refresh"};

	public static final String[] CSRF_EXEMPT_ROUTES = Stream
			.concat(Stream.of("/api/v1/webhooks/stripe"), Stream.of(PRE_SESSION_AUTH_ROUTES)).toArray(String[]::new);

	public static final String ERROR_DISPATCH_ROUTE = "/error";

	public static final String[] PUBLIC_ROUTES = Stream
			.concat(Stream.of(PRE_SESSION_AUTH_ROUTES),
					Stream.of("/api/v1/auth/logout", "/api/v1/catalog/**", "/api/v1/webhooks/**", ERROR_DISPATCH_ROUTE))
			.toArray(String[]::new);
}
