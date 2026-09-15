package com.suraksha.backend.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only viewer over the append-only audit trail. Already gated by
 * SecurityConfig's /api/admin/** -> ADMIN rule; @PreAuthorize here is
 * defense-in-depth against that rule ever being loosened by mistake.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/api/admin/audit-logs")
    public Page<AuditLog> list(@RequestParam(required = false) String eventType,
                                @RequestParam(required = false) UUID userId,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "50") int size) {
        int cappedSize = Math.min(size, 200);
        PageRequest pageable = PageRequest.of(page, cappedSize, Sort.by(Sort.Direction.DESC, "occurredAt"));

        if (eventType != null && !eventType.isBlank()) {
            return auditLogRepository.findByEventTypeOrderByOccurredAtDesc(eventType, pageable);
        }
        if (userId != null) {
            return auditLogRepository.findByUserIdOrderByOccurredAtDesc(userId, pageable);
        }
        return auditLogRepository.findAllByOrderByOccurredAtDesc(pageable);
    }
}
