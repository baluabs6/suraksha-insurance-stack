package com.suraksha.backend.documents.dto;

import com.suraksha.backend.documents.DocumentType;
import com.suraksha.backend.documents.PolicyDocument;

import java.time.Instant;
import java.util.UUID;

public record DocumentSummary(
        UUID id,
        UUID policyId,
        UUID claimId,
        DocumentType documentType,
        String fileName,
        String contentType,
        Instant uploadedAt
) {
    public static DocumentSummary from(PolicyDocument doc) {
        return new DocumentSummary(doc.getId(), doc.getPolicyId(), doc.getClaimId(),
                doc.getDocumentType(), doc.getFileName(), doc.getContentType(), doc.getUploadedAt());
    }
}
