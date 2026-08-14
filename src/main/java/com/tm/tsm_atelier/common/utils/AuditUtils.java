package com.tm.tsm_atelier.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuditUtils {

	/**
	 * Quem disparou a ação, para o rastro de auditoria. Lido do contexto de
	 * segurança em vez de virar parâmetro do método porque é informação de log, e
	 * não de negócio — a assinatura pública dos serviços não deveria mudar por
	 * causa disso.
	 *
	 * Rastro, e não regra: nenhuma autorização depende deste valor, quem decide
	 * isso é o SecurityConfig. Cai em "system" quando não há requisição autenticada
	 * por trás, como no scheduler de expiração de pedidos.
	 */
	public static String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication == null ? "system" : authentication.getName();
	}

	private AuditUtils() {
	}
}
