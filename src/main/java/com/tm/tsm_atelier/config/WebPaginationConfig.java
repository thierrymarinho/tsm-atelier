package com.tm.tsm_atelier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Teto de itens por página, para todas as rotas paginadas.
 *
 *
 * O default do Spring é 2000. Numa rota pública isso é uma página de catálogo
 * inteira por requisição — e como a chave do cache catalog_products inclui o
 * Pageable, cada tamanho distinto pedido vira uma entrada nova no Redis. O teto
 * fecha os dois lados de uma vez.
 *
 *
 * Isto não é a propriedade spring.data.web.pageable.max-page-size de propósito.
 * Aquela propriedade só é lida por SpringDataWebAutoConfiguration, que recua
 * quando a aplicação declara @EnableSpringDataWebSupport — o que ela faz, para
 * serializar Page via DTO. Escrita no application.yaml, a propriedade carrega
 * sem erro e não faz absolutamente nada; foi verificado que ?size=150
 * continuava devolvendo 150 itens com ela no
 * arquivo. @EnableSpringDataWebSupport consome este customizer, e é por ele que
 * o limite passa a valer.
 */
@Configuration
public class WebPaginationConfig {

	private static final int MAX_PAGE_SIZE = 100;

	@Bean
	PageableHandlerMethodArgumentResolverCustomizer pageableMaxSizeCustomizer() {
		return resolver -> resolver.setMaxPageSize(MAX_PAGE_SIZE);
	}
}
