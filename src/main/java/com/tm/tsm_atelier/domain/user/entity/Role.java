package com.tm.tsm_atelier.domain.user.entity;

public enum Role {

	CUSTOMER,

	ADMIN,

	/**
	 * Leitura do painel, para demonstracao publica. Alcanca apenas os GET de
	 * dashboard, produtos, colecoes e auditoria — a lista esta em
	 * SecurityConstants.ADMIN_VIEWER_ROUTES.
	 *
	 *
	 * Pedidos ficaram de fora de proposito, e o motivo nao e o que a tela mostra: e
	 * o filtro de busca. OrderSpecification.search casa por substring no e-mail e
	 * no nome do cliente, entao mascarar a resposta nao fecha nada — quem consulta
	 * reconstroi o endereco alongando o termo e observando quais consultas devolvem
	 * resultado. E a mesma enumeracao que o cadastro deixou de permitir, por outra
	 * porta.
	 */
	ADMIN_VIEWER
}
