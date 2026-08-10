package com.tm.tsm_atelier.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final AccessTokenDenylist accessTokenDenylist;

	public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
			AccessTokenDenylist accessTokenDenylist) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.accessTokenDenylist = accessTokenDenylist;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String jwt = extractAccessToken(request);

		if (jwt == null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			String userEmail = jwtService.extractUsername(jwt);

			if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null
					&& jwtService.isTokenValid(jwt, userEmail) && !isRevoked(jwt)) {

				UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
						null, userDetails.getAuthorities());

				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}
		} catch (Exception e) {
			logger.debug("Invalid JWT token: " + e.getMessage());
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Falha aberto de proposito. O denylist so cobre logout voluntario, entao
	 * indisponibilidade dele significa que um token descartado sobrevive pelo que
	 * resta da sua validade — no maximo alguns minutos. Falhar fechado custaria o
	 * contrario: ninguem autentica, e o site inteiro cai por causa de uma
	 * dependencia que existe apenas para revogar.
	 *
	 * Em warn, e nao em debug: o sistema esta rodando com uma garantia a menos, e
	 * isso precisa aparecer em algum lugar.
	 */
	private boolean isRevoked(String jwt) {
		try {
			return accessTokenDenylist.isRevoked(jwt);
		} catch (DataAccessException e) {
			logger.warn("Denylist unavailable; accepting the token without checking for revocation", e);
			return false;
		}
	}

	private String extractAccessToken(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return null;
		}

		for (Cookie cookie : request.getCookies()) {
			if (SecurityConstants.ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return null;
	}
}
