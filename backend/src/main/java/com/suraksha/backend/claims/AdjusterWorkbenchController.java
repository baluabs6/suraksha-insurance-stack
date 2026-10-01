package com.suraksha.backend.claims;

import com.suraksha.backend.audit.AuditService;
import com.suraksha.backend.documents.DocumentType;
import com.suraksha.backend.documents.PolicyDocumentRepository;
import com.suraksha.backend.documents.dto.DocumentSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Adjuster tools: a prioritised queue with SLA tracking, self-assignment, and access to claim documents. */
@RestController
@RequestMapping("/api/adjuster")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CLAIMS_ADJUSTER', 'ADMIN')")
public class AdjusterWorkbenchController {

    private static final List<ClaimStatus> OPEN = List.of(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW);

    private final ClaimRepository claimRepository;
    private final PolicyDocumentRepository documentRepository;
    private final AuditService auditService;

    public record WorkItem(UUID claimId, String claimType, BigDecimal claimAmount, LocalDate incidentDate,
                           ClaimStatus status, String riskLevel, Double riskScore, Instant submittedAt,
                           long hoursOpen, long slaHours, boolean slaBreached, UUID assignedAdjusterId,
                           double priority) {}

    @GetMapping("/claims/workbench")
    public List<WorkItem> workbench(@RequestParam(defaultValue = "false") boolean mine, Authentication auth) {
        UUID me = UUID.fromString(auth.getName());
        Instant now = Instant.now();
        return claimRepository.findByStatusIn(OPEN).stream()
                .filter(c -> !mine || me.equals(c.getAssignedAdjusterId()))
                .map(c -> {
                    var s = ClaimPriority.score(c.getRiskScore(), c.getSubmittedAt(), c.getAssignedAdjusterId() != null, now);
                    return new WorkItem(c.getId(), c.getClaimType(), c.getClaimAmount(), c.getIncidentDate(), c.getStatus(),
                            c.getRiskLevel(), c.getRiskScore(), c.getSubmittedAt(), s.hoursOpen(), ClaimPriority.SLA_HOURS,
                            s.slaBreached(), c.getAssignedAdjusterId(), s.priority());
                })
                .sorted(Comparator.comparingDouble(WorkItem::priority).reversed())
                .toList();
    }

    @PostMapping("/claims/{id}/assign")
    public ResponseEntity<?> assignToMe(@PathVariable UUID id, Authentication auth) {
        Claim claim = claimRepository.findById(id).orElse(null);
        if (claim == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Claim not found."));
        }
        UUID me = UUID.fromString(auth.getName());
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (claim.getAssignedAdjusterId() != null && !claim.getAssignedAdjusterId().equals(me) && !admin) {
            return ResponseEntity.status(409).body(Map.of("message", "This claim is already assigned to another adjuster."));
        }
        claim.setAssignedAdjusterId(me);
        claim.setAssignedAt(Instant.now());
        claimRepository.save(claim);
        auditService.record(me, "CLAIM_ASSIGNED", null, null, "claimId=" + id);
        return ResponseEntity.ok(Map.of("claimId", id, "assignedAdjusterId", me));
    }

    @GetMapping("/claims/{id}/documents")
    public ResponseEntity<?> documents(@PathVariable UUID id) {
        if (!claimRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("message", "Claim not found."));
        }
        return ResponseEntity.ok(documentRepository.findByClaimIdOrderByUploadedAtDesc(id).stream()
                .map(DocumentSummary::from).toList());
    }

    /** Opening a customer's document is audit-logged, like viewing health claim details. */
    @GetMapping("/documents/{docId}/content")
    public ResponseEntity<?> documentContent(@PathVariable UUID docId, Authentication auth) {
        return documentRepository.findById(docId)
                .filter(d -> d.getClaimId() != null && d.getDocumentType() != DocumentType.KYC_ID_PROOF
                        && d.getDocumentType() != DocumentType.KYC_ADDRESS_PROOF)
                .<ResponseEntity<?>>map(d -> {
                    auditService.record(UUID.fromString(auth.getName()), "CLAIM_DOCUMENT_VIEWED", null, null,
                            "documentId=" + d.getId() + ", claimId=" + d.getClaimId());
                    return ResponseEntity.ok(Map.of("fileName", d.getFileName(), "contentType", d.getContentType(),
                            "base64Content", d.getBase64Content()));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Document not found.")));
    }
}
