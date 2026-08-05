package com.tm.tsm_atelier.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
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
	protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		// O cookie httpOnly é a única entrada aceita. Havia um fallback para
		// Authorization: Bearer, removido de propósito — ele era uma segunda porta
		// para a mesma credencial, e fora do modelo de ameaça em que o resto do
		// fluxo foi desenhado: o SameSite do cookie não cobre um header, e um token
		// que vaze em log, URL ou proxy vira sessão utilizável por essa via.
		String jwt = null;

		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if ("access_token".equals(cookie.getName())) {
					jwt = cookie.getValue();
					break;
				}
			}
		}

		// No cookie found, proceed to next filter (Spring will block protected routes)
		if (jwt == null) {
			filterChain.doFilter(request, response);
			return;
		}

		// Um token revogado no logout continua tecnicamente válido — assinatura boa,
		// dentro da validade. Só o denylist sabe que ele não vale mais.
		if (accessTokenDenylist.isRevoked(jwt)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			String userEmail = jwtService.extractUsername(jwt);

			if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				// A role vem de userDetails.getAuthorities(), ou seja, do banco — nunca
				// do claim do token. É o que faz um admin rebaixado perder o acesso na
				// requisição seguinte, em vez de só quando o token expira. Havia aqui um
				// extractRole(jwt) atribuído e nunca usado; foi removido para ninguém
				// achar que a role do token serve para autorizar.
				if (jwtService.isTokenValid(jwt, userEmail)) {
					UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
							null, userDetails.getAuthorities());

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
