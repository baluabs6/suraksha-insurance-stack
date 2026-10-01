package com.suraksha.backend.policy;

import com.suraksha.backend.audit.AuditService;
import com.suraksha.backend.claims.ClaimRepository;
import com.suraksha.backend.claims.ClaimStatus;
import com.suraksha.backend.notifications.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** After-sale servicing: cancellation with a refund estimate, and nominee changes on life policies. */
@RestController
@RequestMapping("/api/policies/{id}")
@RequiredArgsConstructor
public class PolicyServiceController {

    static final List<String> NOMINEE_RELATIONSHIPS = List.of("Spouse", "Child", "Parent", "Sibling", "Other");

    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Value("${app.policy.cancellation-fee-percent:10}")
    private int cancellationFeePercent;

    @Data
    public static class NomineeRequest {
        @NotBlank(message = "Enter the nominee's name.")
        @Size(max = 100)
        @Pattern(regexp = "^[\\p{L} .'-]{2,100}$", message = "The nominee's name can only contain letters, spaces and . ' -")
        private String nomineeName;

        @NotBlank(message = "Choose the nominee's relationship to you.")
        private String nomineeRelationship;
    }

    private record Preview(RefundCalculator.Result refund, String blocker) {}

    @GetMapping("/cancellation-quote")
    public ResponseEntity<?> cancellationQuote(@PathVariable UUID id, Authentication auth) {
        Policy policy = owned(id, auth);
        if (policy == null) return notFound();
        Preview preview = preview(policy);
        if (preview.blocker() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", preview.blocker()));
        }
        return ResponseEntity.ok(preview.refund());
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(@PathVariable UUID id, Authentication auth) {
        Policy policy = owned(id, auth);
        if (policy == null) return notFound();
        Preview preview = preview(policy);
        if (preview.blocker() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", preview.blocker()));
        }

        policy.setStatus(PolicyStatus.CANCELLED);
        policy.setCancelledAt(Instant.now());
        policy.setRefundAmount(preview.refund().refund());
        policyRepository.save(policy);

        UUID userId = UUID.fromString(auth.getName());
        auditService.record(userId, "POLICY_CANCELLED", null, null,
                "policyId=" + policy.getId() + ", refund=" + preview.refund().refund());
        notificationService.create(userId, "POLICY_CANCELLED", "Policy cancelled",
                policy.getPolicyNumber() + " has been cancelled. Refund due: ₹" + preview.refund().refund().toPlainString() + ".",
                policy.getId());

        return ResponseEntity.ok(Map.of("policy", policy, "refund", preview.refund(),
                "message", "Your policy is cancelled. Any refund is processed by our team; no payout is triggered automatically."));
    }

    @PatchMapping("/nominee")
    public ResponseEntity<?> updateNominee(@PathVariable UUID id, @Valid @RequestBody NomineeRequest req, Authentication auth) {
        Policy policy = owned(id, auth);
        if (policy == null) return notFound();
        if (policy.getType() != PolicyType.LIFE) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nominees can only be set on life policies."));
        }
        if (policy.getStatus() != PolicyStatus.ACTIVE && policy.getStatus() != PolicyStatus.PENDING_PAYMENT) {
            return ResponseEntity.badRequest().body(Map.of("message", "This policy is no longer in force."));
        }
        String relationship = NOMINEE_RELATIONSHIPS.stream()
                .filter(r -> r.equalsIgnoreCase(req.getNomineeRelationship().trim())).findFirst().orElse(null);
        if (relationship == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Choose a valid relationship."));
        }

        Map<String, Object> attributes = new LinkedHashMap<>(policy.getAttributes() == null ? Map.of() : policy.getAttributes());
        attributes.put("nomineeName", req.getNomineeName().trim());
        attributes.put("nomineeRelationship", relationship);
        policy.setAttributes(attributes);
        policyRepository.save(policy);

        auditService.record(UUID.fromString(auth.getName()), "NOMINEE_CHANGED", null, null, "policyId=" + policy.getId());
        return ResponseEntity.ok(policy);
    }

    private Preview preview(Policy policy) {
        if (policy.getStatus() == PolicyStatus.CANCELLED || policy.getStatus() == PolicyStatus.EXPIRED) {
            return new Preview(null, "This policy is already " + policy.getStatus().name().toLowerCase() + ".");
        }
        boolean inProgress = !claimRepository.findByPolicyIdAndStatusIn(policy.getId(),
                List.of(ClaimStatus.SUBMITTED, ClaimStatus.UNDER_REVIEW, ClaimStatus.APPROVED)).isEmpty();
        if (inProgress) {
            return new Preview(null, "This policy has claims still in progress. You can cancel once they are settled or closed.");
        }
        if (policy.getStatus() == PolicyStatus.PENDING_PAYMENT) {
            return new Preview(RefundCalculator.nothingPaid(), null);
        }
        boolean claimPaid = !claimRepository.findByPolicyIdAndStatusIn(policy.getId(), List.of(ClaimStatus.SETTLED)).isEmpty();
        return new Preview(RefundCalculator.compute(policy.getPremium(), policy.getStartDate(), policy.getEndDate(),
                LocalDate.now(), cancellationFeePercent, claimPaid), null);
    }

    private Policy owned(UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return policyRepository.findById(id).filter(p -> p.getUser().getId().equals(userId)).orElse(null);
    }

    private static ResponseEntity<?> notFound() {
        return ResponseEntity.status(404).body(Map.of("message", "Policy not found for this account."));
    }
}
