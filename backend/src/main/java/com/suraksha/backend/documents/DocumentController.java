package com.suraksha.backend.documents;

import com.suraksha.backend.claims.ClaimRepository;
import com.suraksha.backend.documents.dto.DocumentSummary;
import com.suraksha.backend.documents.dto.DocumentUploadRequest;
import com.suraksha.backend.policy.PolicyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    // ~6 MB of actual file content (base64 inflates size by ~33%). Demo-level
    // guardrail only — see PolicyDocument's class comment for the real fix.
    private static final int MAX_BASE64_LENGTH = 8_000_000;

    private final PolicyDocumentRepository policyDocumentRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;

    @PostMapping
    public ResponseEntity<?> upload(@Valid @RequestBody DocumentUploadRequest req, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());

        if (req.getBase64Content().length() > MAX_BASE64_LENGTH) {
            return ResponseEntity.status(413).body(Map.of("message", "File is too large. Maximum size is about 6 MB."));
        }

        if (req.getPolicyId() != null) {
            boolean ownsPolicy = policyRepository.findById(req.getPolicyId())
                    .map(p -> p.getUser().getId().equals(userId)).orElse(false);
            if (!ownsPolicy) {
                return ResponseEntity.status(404).body(Map.of("message", "Policy not found for this account."));
            }
        }
        if (req.getClaimId() != null) {
            boolean ownsClaim = claimRepository.findById(req.getClaimId())
                    .map(c -> c.getUser().getId().equals(userId)).orElse(false);
            if (!ownsClaim) {
                return ResponseEntity.status(404).body(Map.of("message", "Claim not found for this account."));
            }
        }

        PolicyDocument doc = PolicyDocument.builder()
                .userId(userId)
                .policyId(req.getPolicyId())
                .claimId(req.getClaimId())
                .documentType(req.getDocumentType())
                .fileName(req.getFileName())
                .contentType(req.getContentType())
                .base64Content(req.getBase64Content())
                .build();
        policyDocumentRepository.save(doc);

        return ResponseEntity.status(201).body(DocumentSummary.from(doc));
    }

    @GetMapping
    public List<DocumentSummary> mine(Authentication auth) {
        return policyDocumentRepository.findByUserIdOrderByUploadedAtDesc(UUID.fromString(auth.getName()))
                .stream().map(DocumentSummary::from).toList();
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<?> content(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return policyDocumentRepository.findById(id)
                .filter(d -> d.getUserId().equals(userId))
                .<ResponseEntity<?>>map(d -> ResponseEntity.ok(Map.of(
                        "fileName", d.getFileName(),
                        "contentType", d.getContentType(),
                        "base64Content", d.getBase64Content())))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Document not found for this account.")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return policyDocumentRepository.findById(id)
                .filter(d -> d.getUserId().equals(userId))
                .<ResponseEntity<?>>map(d -> {
                    policyDocumentRepository.delete(d);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Document not found for this account.")));
    }
}
