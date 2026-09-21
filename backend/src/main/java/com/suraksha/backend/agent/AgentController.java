package com.suraksha.backend.agent;

import com.suraksha.backend.claims.Claim;
import com.suraksha.backend.claims.ClaimRepository;
import com.suraksha.backend.policy.Policy;
import com.suraksha.backend.policy.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
public class AgentController {

    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;

    @GetMapping("/api/agent/policies")
    public List<Policy> myPolicies(Authentication auth) {
        return policyRepository.findByAgentIdOrderByStartDateDesc(UUID.fromString(auth.getName()));
    }

    @GetMapping("/api/agent/claims")
    public List<Claim> claimsOnMyPolicies(Authentication auth) {
        UUID agentId = UUID.fromString(auth.getName());
        List<UUID> myPolicyIds = policyRepository.findByAgentIdOrderByStartDateDesc(agentId)
                .stream().map(Policy::getId).toList();
        return claimRepository.findAll().stream()
                .filter(c -> myPolicyIds.contains(c.getPolicy().getId()))
                .sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()))
                .toList();
    }
}
