package com.suraksha.backend.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suraksha.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType type;

    @Column(nullable = false)
    private BigDecimal coverageAmount;

    @Column(nullable = false)
    private BigDecimal premium;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status = PolicyStatus.ACTIVE;
}
