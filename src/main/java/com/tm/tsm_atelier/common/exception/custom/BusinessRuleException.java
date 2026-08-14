package com.tm.tsm_atelier.common.exception.custom;

/**
 * Regra de negócio violada pelo dado que o cliente enviou — resposta 400.
 *
 * <p>
 * Existe para tirar essas validações de {@link IllegalArgumentException}. Antes
 * o handler global mapeava <em>toda</em> IllegalArgumentException para 400, o
 * que funcionava para as regras de domínio e escondia o resto: um IAE lançado
 * de dentro do framework — conversão, parsing, argumento inválido em chamada
 * interna — também virava 400 e sumia do log de erro, reportado ao cliente como
 * se fosse culpa dele.
 */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}
}
