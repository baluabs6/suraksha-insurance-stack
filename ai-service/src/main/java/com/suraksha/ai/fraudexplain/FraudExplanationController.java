package com.suraksha.ai.fraudexplain;

import com.suraksha.ai.client.AnthropicClient;
import com.suraksha.ai.model.ClaimRecord;
import com.suraksha.ai.model.PolicyRecord;
import com.suraksha.ai.repository.ClaimRecordRepository;
import com.suraksha.ai.repository.PolicyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai/fraud-explanation")
@RequiredArgsConstructor
public class FraudExplanationController {

    private static final String SYSTEM_PROMPT = """
            You explain, in plain language, why a fraud-screening model
            assigned a particular risk score to an insurance claim. You are
            given the score, its risk band, and the specific rule-based
            signals that contributed to it — restate and explain them clearly
            in 2-3 sentences for a claims adjuster who will make the actual
            decision.

            Rules:
            - Only reference the signals given to you. Never invent a reason
              not in the list, and never claim there was fraud — a high score
              means "worth a closer look," not "confirmed fraudulent."
            - Never state or imply whether the claim should be approved or
              denied. That decision belongs to the adjuster.
            - If no signals are listed, say plainly that the score is low
              because no rule-based signals were triggered.
            """;

    private final ClaimRecordRepository claimRecordRepository;
    private final PolicyRecordRepository policyRecordRepository;
    private final FraudSignalExplainer fraudSignalExplainer;
    private final AnthropicClient anthropicClient;

    @GetMapping("/{claimId}")
    public ResponseEntity<?> explain(@PathVariable UUID claimId) {
        ClaimRecord claim = claimRecordRepository.findById(claimId).orElse(null);
        if (claim == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Claim not found."));
        }
        if (claim.getRiskScore() == null) {
            return ResponseEntity.status(409).body(Map.of("message",
                    "This claim hasn't been scored yet — fraud-detection-service scores claims "
                            + "asynchronously shortly after submission. Try again in a few seconds."));
        }

        PolicyRecord policy = policyRecordRepository.findById(claim.getPolicyId()).orElse(null);
        List<FraudSignalExplainer.Signal> signals = fraudSignalExplainer.explainSignals(claim, policy);

        String signalList = signals.isEmpty()
                ? "(none triggered)"
                : signals.stream().map(FraudSignalExplainer.Signal::plainLanguage).collect(Collectors.joining("\n- "));

        String userMessage = """
                Claim ID: %s
                Risk score: %.2f
                Risk level: %s

                Signals that contributed:
                - %s

                Explain this to the adjuster.
                """.formatted(claim.getId(), claim.getRiskScore(), claim.getRiskLevel(), signalList);

        String fallback = "AI explanation unavailable (ANTHROPIC_API_KEY not configured). Risk level: "
                + claim.getRiskLevel() + " (score " + claim.getRiskScore() + "). Signals: " + signalList;

        String explanation = anthropicClient.complete(SYSTEM_PROMPT, userMessage, fallback);

        return ResponseEntity.ok(Map.of(
                "claimId", claim.getId(),
                "riskScore", claim.getRiskScore(),
                "riskLevel", claim.getRiskLevel(),
                "signals", signals.stream().map(FraudSignalExplainer.Signal::code).toList(),
                "explanation", explanation.trim(),
                "usedFallback", explanation.equals(fallback)
        ));
    }
}
