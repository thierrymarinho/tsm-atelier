package com.tm.tsm_atelier.domain.admin.repository;

import com.tm.tsm_atelier.domain.admin.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminAuditLogRepository
		extends
			JpaRepository<AdminAuditLog, Long>,
			JpaSpecificationExecutor<AdminAuditLog> {
}
