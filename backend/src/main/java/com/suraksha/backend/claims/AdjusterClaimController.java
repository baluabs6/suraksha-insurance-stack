package com.suraksha.backend.claims;

import com.suraksha.backend.audit.AuditService;
import com.suraksha.backend.claims.dto.ClaimStatusUpdateRequest;
import com.suraksha.backend.notifications.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/adjuster/claims")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CLAIMS_ADJUSTER', 'ADMIN')")
public class AdjusterClaimController {

    private final ClaimRepository claimRepository;
    private final ClaimStatusHistoryRepository claimStatusHistoryRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @GetMapping
    public List<Claim> queue(@RequestParam(required = false) ClaimStatus status,
                              @RequestParam(required = false) String riskLevel) {
        List<Claim> claims = claimRepository.findAll();
        return claims.stream()
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> riskLevel == null || riskLevel.equalsIgnoreCase(c.getRiskLevel()))
                .sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        return claimRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Claim not found.")));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> history(@PathVariable UUID id) {
        if (!claimRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("message", "Claim not found."));
        }
        return ResponseEntity.ok(claimStatusHistoryRepository.findByClaimIdOrderByOccurredAtAsc(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable UUID id,
                                           @Valid @RequestBody ClaimStatusUpdateRequest req,
                                           Authentication auth) {
        Claim claim = claimRepository.findById(id).orElse(null);
        if (claim == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Claim not found."));
        }

        ClaimStatus previous = claim.getStatus();
        if (previous == req.getStatus()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Claim is already in that status."));
        }

        UUID adjusterId = UUID.fromString(auth.getName());

        claim.setStatus(req.getStatus());
        claim.setStatusUpdatedBy(adjusterId);
        claim.setStatusUpdatedAt(Instant.now());
        if (req.getNote() != null && !req.getNote().isBlank()
                && (req.getStatus() == ClaimStatus.APPROVED || req.getStatus() == ClaimStatus.REJECTED
                    || req.getStatus() == ClaimStatus.SETTLED)) {
            claim.setDecisionNotes(req.getNote());
        }
        claimRepository.save(claim);

        claimStatusHistoryRepository.save(ClaimStatusHistory.builder()
                .claimId(claim.getId())
                .fromStatus(previous)
                .toStatus(req.getStatus())
                .changedByUserId(adjusterId)
                .note(req.getNote())
                .build());

        notificationService.create(
                claim.getUser().getId(),
                "CLAIM_STATUS_CHANGED",
                "Claim " + statusLabel(req.getStatus()),
                "Your claim for " + claim.getClaimType().toLowerCase() + " (₹" + claim.getClaimAmount()
                        + ") is now " + statusLabel(req.getStatus()).toLowerCase() + ".",
                claim.getId());

        auditService.record(adjusterId, "CLAIM_STATUS_CHANGED", null, null,
                "claimId=" + claim.getId() + " from=" + previous + " to=" + req.getStatus());

        return ResponseEntity.ok(claim);
    }

    private String statusLabel(ClaimStatus status) {
        return switch (status) {
            case SUBMITTED -> "Submitted";
            case UNDER_REVIEW -> "Under review";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case SETTLED -> "Settled";
        };
    }
}
