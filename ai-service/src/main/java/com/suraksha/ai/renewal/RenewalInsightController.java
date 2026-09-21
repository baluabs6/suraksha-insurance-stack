package com.suraksha.ai.renewal;

import com.suraksha.ai.client.AnthropicClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/ai/renewal-insight")
@RequiredArgsConstructor
public class RenewalInsightController {

    private static final String SYSTEM_PROMPT = """
            You explain, in plain language, what factors around an insurance
            policy's renewal a customer should be aware of. You are given a
            list of factors that were already computed by rules — restate and
            explain them clearly and calmly, in 3-4 sentences. Do not invent
            any factor not in the list, and do not state or imply a specific
            new premium amount — you were not given one.
            """;

    private final AnthropicClient anthropicClient;

    @PostMapping
    public RenewalInsightResponse insight(@Valid @RequestBody RenewalInsightRequest req) {
        List<String> factors = new ArrayList<>();

        String daysNote = "Renewal date not provided.";
        if (req.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), req.getEndDate());
            daysNote = days >= 0
                    ? "Policy renews in %d day%s.".formatted(days, days == 1 ? "" : "s")
                    : "Policy end date has passed — this policy has lapsed.";
            if (days >= 0 && days <= 30) {
                factors.add("Renewal is coming up within 30 days.");
            }
        }

        if (req.getTotalClaimsFiled() == 0) {
            factors.add("No claims filed on this policy during the current term — a clean claims history like this typically supports stable renewal terms.");
        } else {
            factors.add("%d claim(s) filed on this policy during the current term.".formatted(req.getTotalClaimsFiled()));
        }

        if (req.getHighRiskClaims() > 0) {
            factors.add("%d of those claim(s) were flagged high-risk by fraud screening.".formatted(req.getHighRiskClaims()));
        }
        if (req.getMediumRiskClaims() > 0) {
            factors.add("%d of those claim(s) were flagged medium-risk by fraud screening.".formatted(req.getMediumRiskClaims()));
        }

        if (req.getCoverageAmount() != null) {
            factors.add("Current coverage amount: ₹%s.".formatted(req.getCoverageAmount()));
        }

        String factorList = String.join("\n- ", factors);
        String userMessage = """
                Policy: %s (%s), plan %s
                %s

                Factors:
                - %s

                Write the explanation for the customer.
                """.formatted(req.getPolicyId(), req.getType(), req.getPlanName(), daysNote, factorList);

        String fallback = "AI explanation unavailable (ANTHROPIC_API_KEY not configured). "
                + "See the factors listed above, or contact support for details on your renewal.";

        String explanation = anthropicClient.complete(SYSTEM_PROMPT, userMessage, fallback);

        return new RenewalInsightResponse(daysNote, factors, explanation.trim(), explanation.equals(fallback));
    }
}
