package com.suraksha.backend.documents;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policy_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private UUID policyId;
    private UUID claimId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Lob
    @Column(nullable = false)
    private String base64Content;

    @Builder.Default
    @Column(nullable = false)
    private Instant uploadedAt = Instant.now();
}
