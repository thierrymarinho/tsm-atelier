package com.tm.tsm_atelier.domain.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Uma linha por alteração administrativa.
 *
 * <p>
 * Não estende {@code BaseEntity} de propósito. O {@code updated_at} de lá vem
 * com {@code @LastModifiedDate}, e uma data de última modificação num registro
 * de auditoria descreve exatamente o que não pode acontecer. Pelo mesmo motivo
 * a classe é {@code @Immutable} e não tem setters: alterar o passado precisa
 * ser impossível pelo caminho normal, e não apenas desaconselhado.
 */
@Entity
@Table(name = "admin_audit_log")
@Immutable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * O e-mail de quem estava autenticado, ou {@code "system"} quando a mudança
	 * partiu de rotina automática. Guardado como texto, e não como FK para
	 * {@code users}: o rastro precisa continuar legível depois que a conta for
	 * removida, e uma FK faria a exclusão do usuário apagar ou travar a auditoria
	 * dele.
	 */
	@Column(nullable = false, length = 255)
	private String actor;

	@Enumerated(EnumType.STRING)
	@Column(name = "entity_type", nullable = false, length = 32)
	private AuditedEntity entityType;

	@Column(name = "entity_id", nullable = false, length = 64)
	private String entityId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AuditAction action;

	@Column(name = "previous_value", length = 255)
	private String previousValue;

	@Column(name = "new_value", length = 255)
	private String newValue;

	@Column(length = 32)
	private String reason;

	@Column(length = 255)
	private String details;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
