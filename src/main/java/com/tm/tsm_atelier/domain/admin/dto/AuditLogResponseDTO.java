package com.tm.tsm_atelier.domain.admin.dto;

import com.tm.tsm_atelier.domain.admin.entity.AuditAction;
import com.tm.tsm_atelier.domain.admin.entity.AuditedEntity;
import java.time.LocalDateTime;

/**
 * A entidade sai como DTO mesmo sendo imutável e sem relacionamento nenhum.
 * Vale pelo mesmo motivo do MEL-02: enquanto uma entidade JPA é o contrato, uma
 * coluna nova aparece na resposta no mesmo commit em que é criada.
 */
public record AuditLogResponseDTO(Long id, String actor, AuditedEntity entityType, String entityId, AuditAction action,
		String previousValue, String newValue, String reason, String details, LocalDateTime createdAt) {
}
