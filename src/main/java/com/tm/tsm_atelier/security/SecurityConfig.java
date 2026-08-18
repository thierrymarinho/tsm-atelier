package com.tm.tsm_atelier.security;

import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final AccessTokenDenylist accessTokenDenylist;

	public SecurityConfig(JwtService jwtService, UserRepository userRepository,
			AccessTokenDenylist accessTokenDenylist) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
		this.accessTokenDenylist = accessTokenDenylist;
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return username -> userRepository.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
	}

	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter(jwtService, userDetailsService(), accessTokenDenylist);
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CookieCsrfTokenRepository csrfTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieName(SecurityConstants.CSRF_COOKIE);
		repository.setCookieCustomizer(
				cookie -> cookie.secure(true).path("/").sameSite(SecurityConstants.COOKIE_SAME_SITE));
		return repository;
	}

	/**
	 * Sem configuracao de CORS, e de proposito. O front chega por um rewrite que o
	 * faz responder pela mesma origem da API, entao nao existe requisicao
	 * cross-origin legitima a autorizar. Se um dia o front voltar a ter origem
	 * propria, isto volta junto — mas como decisao, nao como sobra.
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
		requestHandler.setCsrfRequestAttributeName("_csrf");

		http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository()).csrfTokenRequestHandler(requestHandler)
				.ignoringRequestMatchers(SecurityConstants.CSRF_EXEMPT_ROUTES))
				.authorizeHttpRequests(req -> req.requestMatchers(SecurityConstants.PUBLIC_ROUTES).permitAll()
						.requestMatchers(HttpMethod.GET, SecurityConstants.ADMIN_VIEWER_ROUTES)
						.hasAnyRole("ADMIN", "ADMIN_VIEWER").requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
						.accessDeniedHandler(new ProblemDetailAccessDeniedHandler()))
				.sessionManagement(AbstractHttpConfigurer::disable).authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

		return http.build();
	}
}
