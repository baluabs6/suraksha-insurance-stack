package com.suraksha.ai.policycompare;

import com.suraksha.ai.client.AnthropicClient;
import com.suraksha.ai.model.PolicyTypeView;
import com.suraksha.ai.recommend.PlanCatalog;
import com.suraksha.ai.security.PromptGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai/policy-comparison")
@RequiredArgsConstructor
public class PolicyComparisonController {

    private static final String SYSTEM_PROMPT = PromptGuard.ANTI_INJECTION_PREAMBLE + """

            You help a customer compare a short list of insurance plans against
            their stated situation. You are given the plans (name, type, what
            each covers) and the customer's own description of their situation.

            Write a short comparison, 3-5 sentences, that:
            - Only uses the coverage details given for each plan — never
              invents a premium, coverage amount, or feature not stated.
            - Points out which plan(s) most directly address what the
              customer described, and why, in plain language.
            - If none of the given plans clearly fit the situation, say so
              plainly rather than forcing a recommendation.
            - Ends by reminding the customer this is a general pointer, not
              a substitute for reading the actual policy wording.
            """;

    private final AnthropicClient anthropicClient;

    @PostMapping
    public PolicyComparisonResponse compare(@Valid @RequestBody PolicyComparisonRequest req) {
        Set<PolicyTypeView> owned = req.getOwnedTypes() == null ? Set.of() : Set.copyOf(req.getOwnedTypes());

        List<PlanCatalog.Plan> candidates = PlanCatalog.ALL.stream()
                .filter(p -> !owned.contains(p.type()))
                .toList();

        List<PolicyComparisonResponse.CandidatePlan> candidatePlans = candidates.stream()
                .map(p -> new PolicyComparisonResponse.CandidatePlan(p.type(), p.name(), p.pitchTemplate()))
                .toList();

        if (candidates.isEmpty()) {
            return new PolicyComparisonResponse(List.of(),
                    "You already hold a policy in every category we offer — there's nothing left to compare "
                            + "against your situation right now.", false);
        }

        String planList = candidates.stream()
                .map(p -> "- %s (%s): %s".formatted(p.name(), p.type(), p.pitchTemplate()))
                .collect(Collectors.joining("\n"));

        String userMessage = "Customer's situation:\n" + PromptGuard.wrapUntrusted(req.getSituation())
                + "\n\nCandidate plans:\n" + planList;

        String fallback = "AI comparison unavailable (ANTHROPIC_API_KEY not configured). "
                + "See the candidate plans listed above and their coverage details.";

        String comparison = anthropicClient.complete(SYSTEM_PROMPT, userMessage, fallback);

        return new PolicyComparisonResponse(candidatePlans, comparison.trim(), comparison.equals(fallback));
    }
}
