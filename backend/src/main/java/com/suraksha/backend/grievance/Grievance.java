package com.suraksha.backend.grievance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.suraksha.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

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

    private UUID relatedClaimId;
    private UUID relatedPolicyId;

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
