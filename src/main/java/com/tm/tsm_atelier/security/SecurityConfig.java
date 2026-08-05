package com.tm.tsm_atelier.security;

import com.tm.tsm_atelier.domain.user.repository.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
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

	/**
	 * Deliberadamente sem @Bean. Todo bean do tipo Filter é registrado também na
	 * cadeia do container servlet, além da cadeia do Spring Security — ou seja,
	 * duas vezes. Como ele estende OncePerRequestFilter, a segunda execução é
	 * ignorada, e se a primeira acontecer fora da cadeia de segurança o
	 * SecurityContextHolderFilter descarta a autenticação que ela acabou de montar.
	 * O sintoma é 401 numa requisição com cookie perfeitamente válido. Construído
	 * aqui, ele existe só onde é usado.
	 */
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
	public CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(allowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		// Authorization saiu junto com o fallback de Bearer: a sessão viaja no cookie
		// httpOnly, que o browser envia sozinho. X-XSRF-TOKEN entrou porque é o
		// header que o CookieCsrfTokenRepository espera de volta — sem ele na lista,
		// o preflight barrava todo POST/PUT/DELETE vindo do SPA em outra origem, e
		// as rotas protegidas por CSRF simplesmente não funcionavam.
		config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
			throws Exception {

		CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
		// set the name of the attribute the CsrfToken will be populated on
		requestHandler.setCsrfRequestAttributeName("_csrf");

		http.csrf(csrf -> csrf
				.csrfTokenRepository(
						org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(requestHandler).ignoringRequestMatchers(SecurityConstants.CSRF_EXEMPT_ROUTES))
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.authorizeHttpRequests(req -> req.requestMatchers(SecurityConstants.PUBLIC_ROUTES).permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN").anyRequest().authenticated())
				// Sem entry point explícito, uma requisição sem sessão cai no padrão do
				// Spring e volta 403. Requisição não autenticada é 401; 403 é para quem
				// se autenticou e mesmo assim não pode.
				.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

		return http.build();
	}
}
