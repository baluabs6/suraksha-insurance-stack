package com.suraksha.fraud.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "claims")
@Data
public class ClaimRecord {
    @Id
    private UUID id;

    @Column(name = "policy_id")
    private UUID policyId;

    private BigDecimal claimAmount;

    @Enumerated(EnumType.STRING)
    private ClaimStatusView status;

    private Double riskScore;
    private String riskLevel;
}
