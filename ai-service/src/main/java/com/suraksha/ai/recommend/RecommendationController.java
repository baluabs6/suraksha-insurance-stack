package com.suraksha.ai.recommend;

import com.suraksha.ai.client.AnthropicClient;
import com.suraksha.ai.model.PolicyTypeView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Deliberately rule-based at its core (own a coverage gap → recommend the
 * matching plan), so recommendations are deterministic and explainable, and
 * the feature works even with no API key configured. The LLM is used only to
 * write the personalized pitch sentence on top of a gap the rules already
 * found — never to decide what to recommend.
 */
@RestController
@RequestMapping("/api/ai/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final AnthropicClient anthropicClient;

    private static final String SYSTEM_PROMPT = """
            You are writing a single, warm, one-sentence recommendation pitch for
            an insurance plan the customer doesn't currently have. Be specific and
            concrete, not salesy. Do not invent a premium amount or coverage figure
            beyond what's given to you. One sentence only.
            """;

    @PostMapping
    public List<Recommendation> recommend(@RequestBody RecommendationRequest req) {
        Set<PolicyTypeView> owned = req.getOwnedTypes() == null ? Set.of() : Set.copyOf(req.getOwnedTypes());
        List<Recommendation> results = new ArrayList<>();

        for (PlanCatalog.Plan plan : PlanCatalog.ALL) {
            if (owned.contains(plan.type())) continue; // already covered — not a gap

            String userMessage = "Plan: %s (%s). What it covers: %s".formatted(
                    plan.name(), plan.type(), plan.pitchTemplate());
            String reason = anthropicClient.complete(SYSTEM_PROMPT, userMessage, plan.pitchTemplate());

            results.add(new Recommendation(plan.type(), plan.name(), reason.trim()));
        }
        return results;
    }
}
