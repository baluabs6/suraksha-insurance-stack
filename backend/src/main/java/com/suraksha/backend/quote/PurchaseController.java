package com.suraksha.backend.quote;

import com.suraksha.backend.audit.AuditService;
import com.suraksha.backend.policy.Policy;
import com.suraksha.backend.policy.PolicyRepository;
import com.suraksha.backend.policy.PolicyStatus;
import com.suraksha.backend.policy.PolicyType;
import com.suraksha.backend.user.User;
import com.suraksha.backend.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

/**
 * Buying a policy. The premium is always recomputed on the server from the customer's answers — a price sent
 * by the browser is never trusted. The policy starts as PENDING_PAYMENT and turns ACTIVE when the payment clears.
 */
@RestController
@RequestMapping("/api/policies/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    static final int MAX_PENDING_POLICIES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final QuoteService quoteService;
    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @PostMapping
    public ResponseEntity<?> purchase(@Valid @RequestBody QuoteRequest req, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Account not found."));
        }

        long pending = policyRepository.findByUserId(userId).stream()
                .filter(p -> p.getStatus() == PolicyStatus.PENDING_PAYMENT).count();
        if (pending >= MAX_PENDING_POLICIES) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "You have " + pending + " policies waiting for payment. Pay for or cancel one before buying another."));
        }

        QuoteService.QuoteResult quote;
        try {
            quote = quoteService.quote(req);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        Policy policy = policyRepository.save(Policy.builder()
                .user(user)
                .policyNumber(newPolicyNumber(quote.type()))
                .planName(quote.planName())
                .type(quote.type())
                .coverageAmount(quote.coverageAmount())
                .premium(quote.premium())
                .startDate(quote.startDate())
                .endDate(quote.endDate())
                .attributes(quote.attributes().isEmpty() ? null : quote.attributes())
                .status(PolicyStatus.PENDING_PAYMENT)
                .build());

        auditService.record(userId, "POLICY_PURCHASE_STARTED", null, null,
                "policyId=" + policy.getId() + ", type=" + policy.getType());
        return ResponseEntity.status(201).body(Map.of("policy", policy, "nextStep", "PAY"));
    }

    private String newPolicyNumber(PolicyType type) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "POL-" + code(type) + "-" + (1_000_000 + RANDOM.nextInt(9_000_000));
            if (!policyRepository.existsByPolicyNumber(candidate)) return candidate;
        }
        throw new IllegalStateException("Couldn't generate a unique policy number.");
    }

    static String code(PolicyType type) {
        return switch (type) {
            case HEALTH -> "HL";
            case MOTOR -> "MT";
            case LIFE -> "LF";
            case TWO_WHEELER -> "TW";
            case TRAVEL -> "TR";
            case HOME -> "HM";
            case PERSONAL_ACCIDENT -> "PA";
            case CYBER -> "CY";
            case GADGET -> "GD";
            case PET -> "PT";
            case BUSINESS -> "BZ";
            case MARINE_CARGO -> "MC";
        };
    }
}
