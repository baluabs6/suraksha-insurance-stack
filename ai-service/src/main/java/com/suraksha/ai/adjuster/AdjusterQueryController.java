package com.suraksha.ai.adjuster;

import com.suraksha.ai.client.AnthropicClient;
import com.suraksha.ai.model.ClaimRecord;
import com.suraksha.ai.repository.ClaimRecordRepository;
import com.suraksha.ai.security.PromptGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai/adjuster")
@RequiredArgsConstructor
public class AdjusterQueryController {

    private static final String SYSTEM_PROMPT = PromptGuard.ANTI_INJECTION_PREAMBLE + """

            You are answering a claims adjuster's question using ONLY the claim
            records provided below — no outside knowledge, no assumptions about
            claims not listed. If the records don't contain enough information
            to answer, say so plainly instead of guessing.

            Rules:
            - Never state or imply whether any claim should be approved or
              denied — you can summarize patterns and facts, not make or
              suggest decisions.
            - Cite claim IDs (or the last 8 characters, for readability) when
              referring to specific claims.
            - Keep the answer to a short paragraph unless the question asks
              for a list.
            """;

    private final ClaimRecordRepository claimRecordRepository;
    private final AnthropicClient anthropicClient;

    @PostMapping("/query")
    public AdjusterQueryResponse query(@Valid @RequestBody AdjusterQueryRequest req) {
        List<ClaimRecord> claims;

        if (req.getPolicyId() != null && !req.getPolicyId().isBlank()) {
            claims = claimRecordRepository.findByPolicyIdOrderByIncidentDateDesc(UUID.fromString(req.getPolicyId()));
        } else if (req.getRiskLevel() != null && !req.getRiskLevel().isBlank()) {
            claims = claimRecordRepository.findByRiskLevelOrderByIncidentDateDesc(req.getRiskLevel().toUpperCase());
        } else {
            claims = claimRecordRepository.findTop25ByOrderByIncidentDateDesc();
        }

        if (claims.size() > 25) {
            claims = claims.subList(0, 25);
        }

        if (claims.isEmpty()) {
            return new AdjusterQueryResponse(
                    "No claims matched that scope, so there's nothing to answer from.", 0, false);
        }

        String claimContext = claims.stream()
                .map(c -> "- id=%s type=%s amount=₹%s incidentDate=%s riskLevel=%s riskScore=%s description=%s".formatted(
                        c.getId(), c.getClaimType(), c.getClaimAmount(), c.getIncidentDate(),
                        c.getRiskLevel() == null ? "unscored" : c.getRiskLevel(),
                        c.getRiskScore() == null ? "n/a" : c.getRiskScore(),
                        PromptGuard.wrapUntrusted(c.getDescription())))
                .collect(Collectors.joining("\n"));

        String userMessage = "Claim records:\n" + claimContext
                + "\n\nAdjuster's question: " + PromptGuard.wrapUntrusted(req.getQuestion());

        String fallback = "AI query assistant unavailable (ANTHROPIC_API_KEY not configured on ai-service). "
                + claims.size() + " matching claim(s) found — please review them directly.";

        String rawAnswer = anthropicClient.complete(SYSTEM_PROMPT, userMessage, fallback);
        String safeFallback = "The assistant's response couldn't be returned as-is because it looked like it was "
                + "making a decision rather than summarizing facts, which this tool never does. "
                + claims.size() + " matching claim(s) found — please review them directly.";
        String answer = PromptGuard.enforceNoDecisionLanguage(rawAnswer, safeFallback);

        return new AdjusterQueryResponse(answer.trim(), claims.size(), answer.equals(fallback));
    }
}
