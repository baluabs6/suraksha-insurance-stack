package com.suraksha.backend.health;

import com.suraksha.backend.audit.AuditService;
import com.suraksha.backend.claims.ClaimRepository;
import com.suraksha.backend.health.dto.CoverageSummary;
import com.suraksha.backend.health.dto.MemberRequest;
import com.suraksha.backend.health.dto.MemberResponse;
import com.suraksha.backend.policy.Policy;
import com.suraksha.backend.policy.PolicyRepository;
import com.suraksha.backend.policy.PolicyType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Insured members and coverage utilisation for the customer's own health policies. */
@RestController
@RequestMapping("/api/policies/{policyId}")
@RequiredArgsConstructor
public class HealthPolicyController {

    static final int MAX_MEMBERS = 6;

    private final PolicyRepository policyRepository;
    private final PolicyMemberRepository memberRepository;
    private final ClaimRepository claimRepository;
    private final CoverageService coverageService;
    private final AuditService auditService;

    @GetMapping("/coverage")
    public ResponseEntity<?> coverage(@PathVariable UUID policyId, Authentication auth) {
        Policy policy = ownedHealthPolicy(policyId, auth);
        if (policy == null) return notFound();
        CoverageSummary summary = coverageService.summarize(policy);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/members")
    public ResponseEntity<?> members(@PathVariable UUID policyId, Authentication auth) {
        Policy policy = ownedHealthPolicy(policyId, auth);
        if (policy == null) return notFound();
        List<MemberResponse> members = memberRepository.findByPolicyIdOrderByCreatedAtAsc(policyId)
                .stream().map(MemberResponse::from).toList();
        return ResponseEntity.ok(members);
    }

    @PostMapping("/members")
    public ResponseEntity<?> addMember(@PathVariable UUID policyId, @Valid @RequestBody MemberRequest req,
                                       Authentication auth) {
        Policy policy = ownedHealthPolicy(policyId, auth);
        if (policy == null) return notFound();

        if (memberRepository.countByPolicyId(policyId) >= MAX_MEMBERS) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "A policy can have at most " + MAX_MEMBERS + " insured members."));
        }
        if (req.relationship() == MemberRelationship.SELF
                && memberRepository.existsByPolicyIdAndRelationship(policyId, MemberRelationship.SELF)) {
            return ResponseEntity.status(409).body(Map.of("message", "This policy already has a member marked as yourself."));
        }

        PolicyMember member = memberRepository.save(PolicyMember.builder()
                .policy(policy)
                .fullName(req.fullName().trim())
                .dateOfBirth(req.dateOfBirth())
                .relationship(req.relationship())
                .preExistingConditions(blankToNull(req.preExistingConditions()))
                .build());

        // Deliberately does not log the declared conditions themselves.
        auditService.record(UUID.fromString(auth.getName()), "POLICY_MEMBER_ADDED", null, null,
                "policyId=" + policyId + " memberId=" + member.getId());
        return ResponseEntity.status(201).body(MemberResponse.from(member));
    }

    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<?> removeMember(@PathVariable UUID policyId, @PathVariable UUID memberId,
                                          Authentication auth) {
        Policy policy = ownedHealthPolicy(policyId, auth);
        if (policy == null) return notFound();

        PolicyMember member = memberRepository.findByIdAndPolicyId(memberId, policyId).orElse(null);
        if (member == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Member not found on this policy."));
        }
        if (claimRepository.existsByMemberId(memberId)) {
            return ResponseEntity.status(409).body(Map.of("message",
                    "This member has claims on file, so they can't be removed."));
        }
        memberRepository.delete(member);
        auditService.record(UUID.fromString(auth.getName()), "POLICY_MEMBER_REMOVED", null, null,
                "policyId=" + policyId + " memberId=" + memberId);
        return ResponseEntity.noContent().build();
    }

    private Policy ownedHealthPolicy(UUID policyId, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return policyRepository.findById(policyId)
                .filter(p -> p.getUser().getId().equals(userId))
                .filter(p -> p.getType() == PolicyType.HEALTH)
                .orElse(null);
    }

    private ResponseEntity<?> notFound() {
        return ResponseEntity.status(404).body(Map.of("message", "Health policy not found for this account."));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
