package com.suraksha.backend.documents;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata + content for a document a customer uploads (KYC proof, a policy
 * schedule, or a bill/estimate/photo attached to a claim).
 *
 * Simplified for a demo: the file content itself is stored as base64 text
 * directly in Postgres rather than an object store. That's fine at demo
 * scale but the honest production fix is: upload to S3/GCS/Azure Blob,
 * store only the object key + a short-lived signed URL here, and enforce a
 * real size limit and virus scan before accepting anything. See
 * DocumentController for the size cap this demo applies as a stopgap.
 */
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
