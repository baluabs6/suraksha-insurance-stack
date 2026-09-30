package com.suraksha.ai.recommend;

import com.suraksha.ai.client.AnthropicClient;
import com.suraksha.ai.model.PolicyTypeView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/ai/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    /** One model call is made per recommendation, so the list stays short however many plans exist. */
    static final int MAX_RECOMMENDATIONS = 3;

    /** Default order in which unowned plans are offered. */
    private static final List<PolicyTypeView> DEFAULT_ORDER = List.of(
            PolicyTypeView.HEALTH, PolicyTypeView.LIFE, PolicyTypeView.PERSONAL_ACCIDENT,
            PolicyTypeView.MOTOR, PolicyTypeView.TWO_WHEELER, PolicyTypeView.HOME, PolicyTypeView.TRAVEL,
            PolicyTypeView.CYBER, PolicyTypeView.GADGET, PolicyTypeView.PET,
            PolicyTypeView.BUSINESS, PolicyTypeView.MARINE_CARGO);

    /** Lines that naturally go with something the customer already holds; these are offered first. */
    private static final Map<PolicyTypeView, List<PolicyTypeView>> AFFINITY = Map.of(
            PolicyTypeView.MOTOR, List.of(PolicyTypeView.PERSONAL_ACCIDENT),
            PolicyTypeView.TWO_WHEELER, List.of(PolicyTypeView.PERSONAL_ACCIDENT),
            PolicyTypeView.HEALTH, List.of(PolicyTypeView.PERSONAL_ACCIDENT, PolicyTypeView.TRAVEL),
            PolicyTypeView.HOME, List.of(PolicyTypeView.CYBER, PolicyTypeView.GADGET),
            PolicyTypeView.TRAVEL, List.of(PolicyTypeView.HEALTH),
            PolicyTypeView.LIFE, List.of(PolicyTypeView.HEALTH),
            PolicyTypeView.BUSINESS, List.of(PolicyTypeView.CYBER, PolicyTypeView.MARINE_CARGO));

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

        for (PolicyTypeView type : rankUnowned(owned)) {
            PlanCatalog.Plan plan = PlanCatalog.forType(type).orElse(null);
            if (plan == null) continue;

            String userMessage = "Plan: %s (%s). What it covers: %s".formatted(
                    plan.name(), plan.type(), plan.pitchTemplate());
            String reason = anthropicClient.complete(SYSTEM_PROMPT, userMessage, plan.pitchTemplate());

            results.add(new Recommendation(plan.type(), plan.name(), reason.trim()));
        }
        return results;
    }

    /** Affinity picks first, then the default order, unowned only, capped at MAX_RECOMMENDATIONS. */
    static List<PolicyTypeView> rankUnowned(Set<PolicyTypeView> owned) {
        Set<PolicyTypeView> ranked = new LinkedHashSet<>();
        for (PolicyTypeView held : owned) {
            ranked.addAll(AFFINITY.getOrDefault(held, List.of()));
        }
        ranked.addAll(DEFAULT_ORDER);
        return ranked.stream().filter(t -> !owned.contains(t)).limit(MAX_RECOMMENDATIONS).toList();
    }
}
