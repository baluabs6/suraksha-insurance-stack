package com.suraksha.ai.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "claims")
@Data
public class ClaimRecord {
    @Id
    private UUID id;

    @Column(name = "policy_id")
    private UUID policyId;

    private String claimType;
    private BigDecimal claimAmount;
    private LocalDate incidentDate;
    private String description;
    private Double riskScore;
    private String riskLevel;

    // Mirrors com.suraksha.backend.claims.ClaimStatus — read-only here, this
    // service never transitions a claim's status itself.
    @Enumerated(EnumType.STRING)
    private ClaimStatusView status;

    // Written by this service after a triage summary is generated.
    private String aiSummary;
    private String aiRecommendation;
}
