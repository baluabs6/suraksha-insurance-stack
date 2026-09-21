package com.suraksha.backend.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column
    private UUID userId;

    @Column(nullable = false)
    private String eventType;

    @Column
    private String ipAddress;

    @Column(length = 1024)
    private String userAgent;

    @Column(length = 2000)
    private String details;
}
