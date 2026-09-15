package com.suraksha.ai.urgency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suraksha.ai.client.AnthropicClient;
import com.suraksha.ai.security.PromptGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Classifies a claim narrative or chat message for operational urgency
 * (routing/triage only) — e.g. an ongoing hospitalization or a claim
 * describing an active emergency should reach a human faster than a routine
 * fender-bender. This is not a clinical or safety judgment: it never
 * diagnoses, assesses risk to a person, or takes any action itself — it only
 * labels text for a human queue to prioritize, and defaults to MEDIUM
 * whenever the signal is ambiguous rather than guessing.
 */
@RestController
@RequestMapping("/api/ai/urgency")
@RequiredArgsConstructor
@Slf4j
public class UrgencyController {

    private static final Set<String> VALID_LEVELS = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private static final String SYSTEM_PROMPT = PromptGuard.ANTI_INJECTION_PREAMBLE + """

            You classify how operationally urgent an insurance claim narrative
            or customer message is, purely for routing to a human queue faster
            or slower. This is not a medical, legal, or safety judgment — you
            are labeling text, not assessing a person.

            Respond with ONLY a single JSON object, no markdown fences:
            {
              "urgencyLevel": "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
              "reason": "<one short factual sentence citing what in the text drove this>"
            }

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
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping
    public UrgencyResponse classify(@Valid @RequestBody UrgencyRequest req) {
        String claimType = req.getClaimType() == null || req.getClaimType().isBlank()
                ? "unspecified" : req.getClaimType();

        String userMessage = "Claim type: " + claimType + "\n\nText to classify:\n"
                + PromptGuard.wrapUntrusted(req.getText());

        String fallbackSentinel = "__FALLBACK__";
        String raw = anthropicClient.complete(SYSTEM_PROMPT, userMessage, fallbackSentinel);

        if (fallbackSentinel.equals(raw) || !anthropicClient.isConfigured()) {
            return new UrgencyResponse("MEDIUM",
                    "AI urgency triage unavailable (ANTHROPIC_API_KEY not configured) — defaulted to MEDIUM.", true);
        }

        try {
            JsonNode node = mapper.readTree(stripCodeFences(raw));
            String level = node.path("urgencyLevel").asText("MEDIUM").toUpperCase();
            if (!VALID_LEVELS.contains(level)) {
                level = "MEDIUM";
            }
            String reason = node.path("reason").asText("No specific reason returned.");
            return new UrgencyResponse(level, reason, false);
        } catch (Exception e) {
            log.warn("Could not parse urgency JSON response, defaulting to MEDIUM.", e);
            return new UrgencyResponse("MEDIUM", "Couldn't parse the classifier's response — defaulted to MEDIUM.", true);
        }
    }

    private static String stripCodeFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\n", "").replaceFirst("```\\s*$", "");
        }
        return t.trim();
    }
}
