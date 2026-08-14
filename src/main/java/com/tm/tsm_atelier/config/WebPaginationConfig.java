package com.tm.tsm_atelier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Teto de itens por página, para todas as rotas paginadas.
 *
 * <p>
 * O default do Spring é 2000. Numa rota pública isso é uma página de catálogo
 * inteira por requisição — e como a chave do cache {@code catalog_products}
 * inclui o {@code Pageable}, cada tamanho distinto pedido vira uma entrada nova
 * no Redis. O teto fecha os dois lados de uma vez.
 *
 * <p>
 * <strong>Isto não é a propriedade
 * {@code spring.data.web.pageable.max-page-size} de propósito.</strong> Aquela
 * propriedade só é lida por {@code SpringDataWebAutoConfiguration}, que recua
 * quando a aplicação declara {@code @EnableSpringDataWebSupport} — o que ela
 * faz, para serializar {@code Page} via DTO. Escrita no
 * {@code application.yaml}, a propriedade carrega sem erro e não faz
 * absolutamente nada; foi verificado que {@code ?size=150} continuava
 * devolvendo 150 itens com ela no arquivo. {@code @EnableSpringDataWebSupport}
 * consome este customizer, e é por ele que o limite passa a valer.
 */
@Configuration
public class WebPaginationConfig {

	private static final int MAX_PAGE_SIZE = 100;

	@Bean
	PageableHandlerMethodArgumentResolverCustomizer pageableMaxSizeCustomizer() {
		return resolver -> resolver.setMaxPageSize(MAX_PAGE_SIZE);
	}
}
