package com.suraksha.backend.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void record(UUID userId, String eventType, String ipAddress, String userAgent, String details) {
        AuditLog log = AuditLog.builder()
                .occurredAt(Instant.now())
                .userId(userId)
                .eventType(eventType)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }
}
