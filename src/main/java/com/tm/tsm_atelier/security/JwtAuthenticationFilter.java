package com.tm.tsm_atelier.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String jwt = null;

		// Try to extract the token from the HttpOnly cookie
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if ("access_token".equals(cookie.getName())) {
					jwt = cookie.getValue();
					break;
				}
			}
		}

		// Fallback to Authorization Bearer header if cookie is missing
		if (jwt == null) {
			String authHeader = request.getHeader("Authorization");
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				jwt = authHeader.substring(7);
			}
		}

		// No cookie found, proceed to next filter (Spring will block protected routes)
		if (jwt == null) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			String userEmail = jwtService.extractUsername(jwt);

			if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				String role = jwtService.extractRole(jwt);

				if (jwtService.isTokenValid(jwt, userEmail)) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userEmail,
							null, List.of(new SimpleGrantedAuthority(role)));

					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		} catch (Exception e) {
			// Invalid or expired token — proceed without authentication
			logger.debug("Invalid JWT token: " + e.getMessage());
		}

		filterChain.doFilter(request, response);
	}
}
