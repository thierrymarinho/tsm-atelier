package com.tm.tsm_atelier.domain.admin.controller.v1;

import com.tm.tsm_atelier.common.web.SortWhitelist;
import com.tm.tsm_atelier.domain.admin.dto.AuditLogResponseDTO;
import com.tm.tsm_atelier.domain.admin.dto.AuditLogSearchFilter;
import com.tm.tsm_atelier.domain.admin.service.AuditService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A tela de histórico do painel. Só leitura, e é isto que a torna confiável:
 * não existe rota para criar, editar ou apagar uma linha de auditoria, então
 * quem tem acesso ao painel não tem como reescrever o próprio rastro.
 */
@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
public class AuditLogController {

	/**
	 * details e os valores antes/depois ficam de fora de propósito: ordenar por
	 * texto livre não significa nada, e a lista é cronológica por natureza.
	 */
	private static final Set<String> SORTABLE_FIELDS = Set.of("id", "createdAt", "actor", "action", "entityType");

	private final AuditService auditService;

	@GetMapping
	public ResponseEntity<Page<AuditLogResponseDTO>> search(@ModelAttribute AuditLogSearchFilter filter,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ResponseEntity.ok(auditService.search(filter, SortWhitelist.validate(pageable, SORTABLE_FIELDS)));
	}
}
