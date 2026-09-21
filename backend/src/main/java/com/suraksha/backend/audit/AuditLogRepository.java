package com.suraksha.backend.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByEventTypeOrderByOccurredAtDesc(String eventType, Pageable pageable);
    Page<AuditLog> findByUserIdOrderByOccurredAtDesc(UUID userId, Pageable pageable);
    Page<AuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
