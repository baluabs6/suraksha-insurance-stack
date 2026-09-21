package com.suraksha.ai.triage;

import com.suraksha.ai.client.AnthropicClient;
import com.suraksha.ai.events.ClaimFlaggedEvent;
import com.suraksha.ai.model.ClaimRecord;
import com.suraksha.ai.model.PolicyRecord;
import com.suraksha.ai.repository.ClaimRecordRepository;
import com.suraksha.ai.repository.PolicyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimTriageService {

    private static final String SYSTEM_PROMPT = """
            You are a claims triage assistant for an Indian insurance company.
            You write short, factual briefing notes for human claims adjusters —
            never a decision, never a recommendation to approve or deny, just the
            facts an adjuster needs and what to check next. Two short paragraphs
            maximum. Do not address the policyholder; this note is internal.
            """;

    private final ClaimRecordRepository claimRecordRepository;
    private final PolicyRecordRepository policyRecordRepository;
    private final AnthropicClient anthropicClient;

    public void handleClaimFlagged(ClaimFlaggedEvent event) {
        ClaimRecord claim = claimRecordRepository.findById(event.claimId()).orElse(null);
        if (claim == null) {
            log.warn("Claim {} not found for triage summary — skipping.", event.claimId());
            return;
        }
        PolicyRecord policy = policyRecordRepository.findById(claim.getPolicyId()).orElse(null);

        String userMessage = buildPrompt(event, claim, policy);
        String fallback = "Claim flagged as %s risk (%.0f%%) for: %s. Manual review recommended — "
                .formatted(event.riskLevel(), event.riskScore() * 100, String.join(", ", event.flags()))
                + "AI summary unavailable (ANTHROPIC_API_KEY not configured).";

        String summary = anthropicClient.complete(SYSTEM_PROMPT, userMessage, fallback);

        claim.setAiSummary(summary);
        claimRecordRepository.save(claim);
        log.info("Triage summary written for claim {}", claim.getId());
    }

    private String buildPrompt(ClaimFlaggedEvent event, ClaimRecord claim, PolicyRecord policy) {
        return """
                Claim ID: %s
                Claim type: %s
                Claim amount: ₹%s
                Policy coverage: ₹%s
                Incident date: %s
                Description given by claimant: %s

                Risk level: %s (score %.2f)
                Flags raised by the fraud model: %s

                Write the internal triage note.
                """.formatted(
                claim.getId(),
                claim.getClaimType(),
                claim.getClaimAmount(),
                policy != null ? policy.getCoverageAmount() : "unknown",
                claim.getIncidentDate(),
                claim.getDescription(),
                event.riskLevel(),
                event.riskScore(),
                String.join(", ", event.flags())
        );
    }
}
