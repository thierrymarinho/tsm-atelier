package com.tm.tsm_atelier.domain.admin.repository;

import com.tm.tsm_atelier.domain.admin.entity.AdminAuditLog;
import com.tm.tsm_atelier.domain.admin.entity.AuditAction;
import com.tm.tsm_atelier.domain.admin.entity.AuditedEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecification {

	public static Specification<AdminAuditLog> hasEntityType(AuditedEntity entityType) {
		return (root, query, cb) -> entityType == null ? null : cb.equal(root.get("entityType"), entityType);
	}

	/**
	 * Comparação exata, e não {@code LIKE}: o id vem de um link da tela de edição,
	 * nunca digitado, e casar por prefixo faria o histórico do produto 4 incluir o
	 * do produto 42.
	 */
	public static Specification<AdminAuditLog> hasEntityId(String entityId) {
		return (root, query,
				cb) -> entityId == null || entityId.isBlank() ? null : cb.equal(root.get("entityId"), entityId.trim());
	}

	/**
	 * Este, sim, por trecho: o operador digita "maria" e não o e-mail inteiro.
	 */
	public static Specification<AdminAuditLog> hasActor(String actor) {
		return (root, query, cb) -> actor == null || actor.isBlank()
				? null
				: cb.like(cb.lower(root.get("actor")), "%" + actor.trim().toLowerCase() + "%");
	}

	public static Specification<AdminAuditLog> hasAction(AuditAction action) {
		return (root, query, cb) -> action == null ? null : cb.equal(root.get("action"), action);
	}

	public static Specification<AdminAuditLog> createdFrom(LocalDate from) {
		return (root, query,
				cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
	}

	/** O dia final entra inteiro, pelo mesmo motivo do filtro de pedidos. */
	public static Specification<AdminAuditLog> createdTo(LocalDate to) {
		return (root, query,
				cb) -> to == null ? null : cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
	}

	private AuditLogSpecification() {
	}
}
