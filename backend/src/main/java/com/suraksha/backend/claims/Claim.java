package com.suraksha.backend.claims;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suraksha.backend.policy.Policy;
import com.suraksha.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "email", "role", "kycVerified", "createdAt"})
    private User user;

    @Column(nullable = false)
    private String claimType;

    @Column(nullable = false)
    private BigDecimal claimAmount;

    @Column(nullable = false)
    private LocalDate incidentDate;

    @Column(nullable = false, length = 2000)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Builder.Default
    @Column(nullable = false)
    private Instant submittedAt = Instant.now();

    // Populated asynchronously by the fraud-detection service after it
    // consumes the claim.submitted event. Null until scored.
    private Double riskScore;

    private String riskLevel;

    // Populated asynchronously by ai-service after a flagged claim triggers
    // a triage summary. Null for claims that were never flagged.
    @Column(length = 2000)
    private String aiSummary;

    @Column(length = 2000)
    private String aiRecommendation;

    // Internal note an adjuster leaves when moving a claim to APPROVED/REJECTED/
    // SETTLED — never shown to the customer directly, but used as grounding
    // context when ai-service drafts the customer-facing decision letter.
    @Column(length = 2000)
    private String decisionNotes;

    @Column(name = "status_updated_by")
    private UUID statusUpdatedBy;

    private Instant statusUpdatedAt;
}
