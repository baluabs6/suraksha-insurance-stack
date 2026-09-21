package com.suraksha.ai.churn;

import com.suraksha.ai.client.AnthropicClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/ai/churn-risk")
@RequiredArgsConstructor
public class ChurnRiskController {

    private static final String SYSTEM_PROMPT = """
            You are writing short retention-outreach guidance for an insurance
            agent about one of their customers, based on a churn-risk band and
            factors that were already computed by rules. In 2-3 sentences,
            suggest a plausible reason for the risk and a concrete, low-key
            outreach idea (e.g. a check-in call, clarifying a rejected claim).
            Do not invent facts not in the factor list, do not promise a
            discount or premium change, and do not contact the customer
            yourself — this is guidance for the agent only.
            """;

    private final AnthropicClient anthropicClient;

    @PostMapping
    public ChurnRiskResponse score(@Valid @RequestBody ChurnRiskRequest req) {
        List<String> factors = new ArrayList<>();
        int weight = 0;

        if (req.getRejectedClaims() > 0) {
            factors.add("%d claim(s) on this policy were rejected.".formatted(req.getRejectedClaims()));
            weight += 2 * req.getRejectedClaims();
        }
        if (req.getHighRiskClaims() > 0) {
            factors.add("%d claim(s) were flagged high-risk by fraud screening.".formatted(req.getHighRiskClaims()));
            weight += req.getHighRiskClaims();
        }
        if (req.getMediumRiskClaims() > 0) {
            factors.add("%d claim(s) were flagged medium-risk by fraud screening.".formatted(req.getMediumRiskClaims()));
            weight += req.getMediumRiskClaims();
        }
        if (req.getOpenGrievances() > 0) {
            factors.add("%d open complaint(s) are unresolved on this account.".formatted(req.getOpenGrievances()));
            weight += 2 * req.getOpenGrievances();
        }
        if (req.getTotalClaimsFiled() >= 3) {
            factors.add("%d claims filed in the current term is on the high side.".formatted(req.getTotalClaimsFiled()));
            weight += 1;
        }
        if (req.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), req.getEndDate());
            if (days < 0) {
                factors.add("Policy end date has already passed — this policy may have lapsed.");
                weight += 3;
            } else if (days <= 15) {
                factors.add("Renewal is due within 15 days.");
                weight += 1;
            }
        }
        if (factors.isEmpty()) {
            factors.add("No claim, complaint, or timing risk signals found — this looks like a stable renewal.");
        }

        String band = weight >= 4 ? "HIGH" : weight >= 2 ? "MEDIUM" : "LOW";

        String factorList = String.join("\n- ", factors);
        String userMessage = """
                Policy: %s (%s), plan %s
                Churn risk band: %s

                Factors:
                - %s

                Write the outreach guidance for the agent.
                """.formatted(req.getPolicyId(), req.getType(), req.getPlanName(), band, factorList);

        String fallback = "AI guidance unavailable (ANTHROPIC_API_KEY not configured). "
                + "Risk band: " + band + ". See the factors listed above.";

        String narrative = anthropicClient.complete(SYSTEM_PROMPT, userMessage, fallback);

        return new ChurnRiskResponse(band, factors, narrative.trim(), narrative.equals(fallback));
    }
}
