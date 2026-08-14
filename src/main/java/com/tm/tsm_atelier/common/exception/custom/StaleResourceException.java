package com.tm.tsm_atelier.common.exception.custom;

/**
 * O cliente enviou uma alteração baseada numa versão que já não é a atual —
 * resposta 409.
 *
 * O caso concreto é o formulário de produto do admin: ele carrega o estoque,
 * fica aberto enquanto clientes compram, e salva um valor absoluto calculado
 * sobre uma leitura vencida. Sem essa checagem o salvamento ressuscitava
 * unidades já vendidas em silêncio.
 */
public class StaleResourceException extends RuntimeException {

	public StaleResourceException(String message) {
		super(message);
	}
}
