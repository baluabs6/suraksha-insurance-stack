package com.suraksha.backend.health;

import com.suraksha.backend.policy.Policy;
import com.suraksha.backend.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A person insured under a health policy (self, spouse, children, parents on a floater). */
@Entity
@Table(name = "policy_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRelationship relationship;

    /** Free-text list of declared conditions, separated by commas or semicolons. Health data: encrypted at rest. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 4000)
    private String preExistingConditions;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
