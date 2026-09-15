package com.suraksha.backend.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    // Intentionally no update/delete-oriented query methods — see AuditLog's
    // javadoc. Read access for future admin tooling can be added here later.
}
