package com.suraksha.backend.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only security/business event trail. The application only ever
 * INSERTs rows here — see db/audit-hardening.sql for the DB grant change
 * that makes UPDATE/DELETE actually impossible for the app's own DB user,
 * not just "not called from code".
 */
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

    /** e.g. LOGIN_SUCCESS, LOGIN_FAILURE, LOGIN_LOCKED, MFA_ENABLED, MFA_CHALLENGE_FAILED,
     *  REFRESH_REUSE_DETECTED, NEW_DEVICE, CLAIM_FILED, PAYMENT_CONFIRMED, ADJUSTER_QUERY */
    @Column(nullable = false)
    private String eventType;

    @Column
    private String ipAddress;

    @Column(length = 1024)
    private String userAgent;

    @Column(length = 2000)
    private String details;
}
