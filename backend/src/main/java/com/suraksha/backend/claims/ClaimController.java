package com.suraksha.backend.claims;

import com.suraksha.backend.claims.dto.ClaimRequest;
import com.suraksha.backend.claims.events.ClaimEventPublisher;
import com.suraksha.backend.policy.Policy;
import com.suraksha.backend.policy.PolicyRepository;
import com.suraksha.backend.user.User;
import com.suraksha.backend.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final ClaimEventPublisher claimEventPublisher;
    private final ClaimStatusHistoryRepository claimStatusHistoryRepository;

    @GetMapping
    public List<Claim> myClaims(Authentication auth) {
        return claimRepository.findByUserIdOrderBySubmittedAtDesc(UUID.fromString(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<?> fileClaim(@Valid @RequestBody ClaimRequest req, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());

        Policy policy = policyRepository.findById(req.getPolicyId())
                .filter(p -> p.getUser().getId().equals(userId))
                .orElse(null);
        if (policy == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Policy not found for this account."));
        }

        User user = userRepository.findById(userId).orElseThrow();

        Claim claim = Claim.builder()
                .policy(policy)
                .user(user)
                .claimType(policy.getType().name())
                .claimAmount(req.getClaimAmount())
                .incidentDate(req.getIncidentDate())
                .description(req.getDescription())
                .status(ClaimStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .build();

        claimRepository.save(claim);
        claimStatusHistoryRepository.save(ClaimStatusHistory.builder()
                .claimId(claim.getId())
                .fromStatus(null)
                .toStatus(ClaimStatus.SUBMITTED)
                .changedByUserId(null)
                .note(null)
                .build());
        claimEventPublisher.publishSubmitted(claim);
        return ResponseEntity.status(201).body(claim);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return claimRepository.findById(id)
                .filter(c -> c.getUser().getId().equals(userId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Claim not found for this account.")));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> history(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        boolean owned = claimRepository.findById(id).map(c -> c.getUser().getId().equals(userId)).orElse(false);
        if (!owned) {
            return ResponseEntity.status(404).body(Map.of("message", "Claim not found for this account."));
        }
        return ResponseEntity.ok(claimStatusHistoryRepository.findByClaimIdOrderByOccurredAtAsc(id));
    }
}
