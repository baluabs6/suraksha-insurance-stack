package com.suraksha.backend.claims;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

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

    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @Column(length = 1000)
    private String note;

    @Builder.Default
    @Column(nullable = false)
    private Instant occurredAt = Instant.now();
}
