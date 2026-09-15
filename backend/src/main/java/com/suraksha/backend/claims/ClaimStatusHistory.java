package com.suraksha.backend.claims;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only trail of every status change a claim goes through, so the
 * customer-facing timeline (and any future SLA/audit reporting) doesn't have
 * to be reconstructed from the audit_log's free-text details. One row is
 * written the moment a claim is filed (SUBMITTED) and one more each time an
 * adjuster moves it forward.
 */
@Entity
@Table(name = "claim_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "claim_id", nullable = false)
    private UUID claimId;

    @Enumerated(EnumType.STRING)
    private ClaimStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus toStatus;

    // Null for the initial SUBMITTED entry (the system, not a person, created it).
    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @Column(length = 1000)
    private String note;

    @Builder.Default
    @Column(nullable = false)
    private Instant occurredAt = Instant.now();
}
