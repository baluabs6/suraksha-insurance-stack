package com.suraksha.fraud.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
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
    private Instant submittedAt;

    /** Type-specific details. Read-only here: this service never writes them back. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", insertable = false, updatable = false)
    private Map<String, Object> details;

    @Enumerated(EnumType.STRING)
    private ClaimStatusView status;

    private Double riskScore;
    private String riskLevel;

    /** Comma-separated codes of the fraud rules that fired. */
    private String riskFlags;
}
