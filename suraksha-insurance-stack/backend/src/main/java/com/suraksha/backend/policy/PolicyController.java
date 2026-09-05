package com.suraksha.backend.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyRepository policyRepository;

    @GetMapping
    public List<Policy> myPolicies(Authentication auth) {
        return policyRepository.findByUserId(UUID.fromString(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return policyRepository.findById(id)
                .filter(p -> p.getUser().getId().equals(userId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Policy not found for this account.")));
    }
}
