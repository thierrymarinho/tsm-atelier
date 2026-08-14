package com.tm.tsm_atelier.domain.admin.dto;

import com.tm.tsm_atelier.domain.admin.entity.AuditAction;
import com.tm.tsm_atelier.domain.admin.entity.AuditedEntity;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Os filtros da tela de histórico, num record pelo mesmo motivo do
 * {@code OrderSearchFilter}: acrescentar um filtro passa a ser uma linha aqui,
 * e não uma alteração espalhada por controller e serviço.
 */
public record AuditLogSearchFilter(AuditedEntity entityType, String entityId, String actor, AuditAction action,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo) {
}
