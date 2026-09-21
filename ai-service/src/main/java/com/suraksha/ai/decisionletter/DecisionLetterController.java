package com.suraksha.ai.decisionletter;

import com.suraksha.ai.client.AnthropicClient;
import com.suraksha.ai.model.ClaimRecord;
import com.suraksha.ai.model.PolicyRecord;
import com.suraksha.ai.repository.ClaimRecordRepository;
import com.suraksha.ai.repository.PolicyRecordRepository;
import com.suraksha.ai.security.PromptGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/ai/decision-letter")
@RequiredArgsConstructor
public class DecisionLetterController {

    private static final Set<String> VALID_DECISIONS = Set.of("APPROVED", "REJECTED", "SETTLED");

    private static final String SYSTEM_PROMPT = PromptGuard.ANTI_INJECTION_PREAMBLE + """

            You draft a formal, courteous customer-facing letter for an
            insurance claim decision an adjuster has already made — you are
            told what the decision is, you never decide or second-guess it.

            Rules:
            - Use only the claim facts given to you (claim type, amount,
              incident date, policy number). Never invent policy terms,
              reasons, or figures not provided.
            - For a REJECTED decision, if no specific reason was given in the
              internal note, state generically that the claim did not meet
              the policy's terms and invite the customer to contact support
              for details — do not guess at a specific reason.
            - For an APPROVED or SETTLED decision, confirm the claim amount
              and next steps (payment processing) in general terms only — do
              not invent a payment date or method.
            - Always include a closing line inviting the customer to contact
              support with questions, and mention they can file a formal
              complaint through the grievance process if unsatisfied.
            - Keep it under 200 words. Do not sign off with a fabricated
              person's name — sign off as "Suraksha Claims Team".
            """;

    private final ClaimRecordRepository claimRecordRepository;
    private final PolicyRecordRepository policyRecordRepository;
    private final AnthropicClient anthropicClient;

    @PostMapping
    public ResponseEntity<?> draft(@Valid @RequestBody DecisionLetterRequest req) {
        String decision = req.getDecision().toUpperCase();
        if (!VALID_DECISIONS.contains(decision)) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "decision must be one of " + VALID_DECISIONS));
        }

        ClaimRecord claim = claimRecordRepository.findById(req.getClaimId()).orElse(null);
        if (claim == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Claim not found."));
        }
        PolicyRecord policy = policyRecordRepository.findById(claim.getPolicyId()).orElse(null);

        String internalNote = req.getInternalNote() == null || req.getInternalNote().isBlank()
                ? "(none provided)" : PromptGuard.wrapUntrusted(req.getInternalNote());

        String userMessage = """
                Decision: %s
                Claim ID: %s
                Claim type: %s
                Claim amount: ₹%s
                Incident date: %s
                Policy number: %s

                Adjuster's internal note (context only, not for verbatim quoting): %s

                Draft the customer-facing letter.
                """.formatted(
                decision, claim.getId(), claim.getClaimType(), claim.getClaimAmount(), claim.getIncidentDate(),
                policy != null ? policy.getPolicyNumber() : "unknown", internalNote);

        String fallback = "AI letter drafting unavailable (ANTHROPIC_API_KEY not configured on ai-service). "
                + "Decision recorded: " + decision + " for claim " + claim.getId()
                + ". Please draft the customer letter manually.";

        String rawLetter = anthropicClient.complete(SYSTEM_PROMPT, userMessage, fallback);
        String safeFallback = "The drafted letter couldn't be returned as-is because it looked like it was "
                + "restating or overriding the decision rather than just announcing it. Decision recorded: "
                + decision + ". Please draft the letter manually.";
        String letter = PromptGuard.enforceNoDecisionLanguage(rawLetter, safeFallback);

        return ResponseEntity.ok(new DecisionLetterResponse(letter.trim(), letter.equals(fallback)));
    }
}
