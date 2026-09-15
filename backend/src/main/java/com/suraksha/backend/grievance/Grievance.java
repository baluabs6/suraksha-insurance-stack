package com.suraksha.backend.grievance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suraksha.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Formal complaint-tracking record. Indian insurers registered with IRDAI
 * are required to run a documented grievance-redressal process (a
 * registration number, a status a customer can check, and a resolution
 * on record) — this is a minimal version of that: no auto-escalation to
 * the IRDAI Integrated Grievance Management System (IGMS) or SLA-breach
 * alerts yet, but the shape (reference number, category, status, resolution
 * note) is the same one a real integration would hang off of.
 */
@Entity
@Table(name = "grievances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grievance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "email", "role", "kycVerified", "createdAt", "panNumber", "aadhaarNumber", "mfaSecret"})
    private User user;

    // Optional link to the claim or policy the complaint concerns.
    private UUID relatedClaimId;
    private UUID relatedPolicyId;

    // e.g. CLAIM_DELAY, CLAIM_REJECTION, MIS_SELLING, PREMIUM_DISPUTE,
    // SERVICE_QUALITY, PAYMENT_ISSUE, OTHER
    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 2000)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GrievanceStatus status = GrievanceStatus.OPEN;

    @Column(length = 2000)
    private String resolutionNote;

    private UUID resolvedByUserId;
    private Instant resolvedAt;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
