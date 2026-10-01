package com.suraksha.backend.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suraksha.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "email", "role", "kycVerified", "createdAt"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    @JsonIgnoreProperties({"passwordHash", "email", "role", "kycVerified", "createdAt"})
    private User agent;

    @Column(nullable = false, unique = true)
    private String policyNumber;

    @Column(nullable = false)
    private String planName;

    // columnDefinition (no CHECK constraint): adding an enum value must never require a schema change again.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(32)")
    private PolicyType type;

    @Column(nullable = false)
    private BigDecimal coverageAmount;

    @Column(nullable = false)
    private BigDecimal premium;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // ---- Health plan terms (nullable: null means "no limit" / the default) ----
    /** Maximum room rent per day covered; null = no cap. */
    private BigDecimal roomRentCapPerDay;

    /** Percentage of each admissible claim the customer pays; null = 0. */
    private Integer coPayPercent;

    /** Days at the start of the policy with no cover for illness; null = 30. */
    private Integer initialWaitingDays;

    /** Months before declared pre-existing conditions are covered; null = 24. */
    private Integer preExistingWaitingMonths;

    /** What is insured, e.g. vehicle registration, trip destination or property address. Optional. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(32)")
    private PolicyStatus status = PolicyStatus.ACTIVE;

    /** Set when the customer cancels; refundAmount is what is owed back (payout is handled outside this service). */
    private java.time.Instant cancelledAt;

    private BigDecimal refundAmount;
}
