package com.suraksha.ai.urgency;

import com.suraksha.ai.client.AnthropicClient;
import com.suraksha.ai.security.PromptGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/ai/urgency")
@RequiredArgsConstructor
public class UrgencyController {

    private static final Set<String> VALID_LEVELS = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private static final String SYSTEM_PROMPT = PromptGuard.ANTI_INJECTION_PREAMBLE + """

            You classify how operationally urgent an insurance claim narrative
            or customer message is, purely for routing to a human queue faster
            or slower. This is not a medical, legal, or safety judgment — you
            are labeling text, not assessing a person.

            Guide (not exhaustive):
            - LOW: routine, already-resolved, or informational (e.g. minor
              cosmetic damage, a completed and settled event).
            - MEDIUM: a normal claim with no unusual time pressure.
            - HIGH: ongoing hospitalization, significant injury, total loss of
              a vehicle or home, or the customer describes real financial
              hardship from the delay.
            - CRITICAL: text suggests an active, ongoing emergency (e.g. "I'm
              at the hospital right now", "my house is flooding as I type
              this") where routing speed genuinely matters.

            Rules:
            - Never diagnose, speculate about someone's mental or physical
              health beyond what's stated, or comment on whether the claim
              is legitimate.
            - If the text mentions self-harm or a threat to someone's safety,
              still just classify urgency as CRITICAL with reason
              "mentions personal safety — route to a human immediately" —
              do not attempt to counsel, assess, or respond to it yourself.
            - Default to MEDIUM if the signal is ambiguous.
            """;

    private final AnthropicClient anthropicClient;

    @PostMapping
    public UrgencyResponse classify(@Valid @RequestBody UrgencyRequest req) {
        String claimType = req.getClaimType() == null || req.getClaimType().isBlank()
                ? "unspecified" : req.getClaimType();

        String userMessage = "Claim type: " + claimType + "\n\nText to classify:\n"
                + PromptGuard.wrapUntrusted(req.getText());

        return anthropicClient.completeStructured(SYSTEM_PROMPT, userMessage, UrgencyAssessment.class)
                .map(a -> {
                    String level = a.urgencyLevel() == null ? "MEDIUM" : a.urgencyLevel().trim().toUpperCase();
                    if (!VALID_LEVELS.contains(level)) {
                        level = "MEDIUM";
                    }
                    String reason = a.reason() == null || a.reason().isBlank()
                            ? "No specific reason returned." : a.reason();
                    return new UrgencyResponse(level, reason, false);
                })
                .orElseGet(() -> new UrgencyResponse("MEDIUM",
                        "AI urgency triage unavailable (ANTHROPIC_API_KEY not configured or the reply couldn't be read) — defaulted to MEDIUM.",
                        true));
    }
}
