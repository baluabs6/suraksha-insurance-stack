package com.suraksha.ai.claimassistant;

import com.suraksha.ai.client.AnthropicClient;
import com.suraksha.ai.security.PromptGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai/claim-assistant")
@RequiredArgsConstructor
@Slf4j
public class ClaimAssistantController {

    private static final String SYSTEM_PROMPT = """
            You turn a customer's plain-English description of an insurance
            incident into a draft claim form. You are given the customer's own
            policies (id, type, policy number, plan name) and their narrative.

            Rules:
            - Never invent an incident date, amount, or detail the customer
              didn't mention. If something's missing, leave it null and add a
              clarifying question instead.
            - matchedPolicyId must be an id from the list given, or null — never
              invent one.
            - clarifyingQuestions should list only what's actually missing or
              ambiguous (e.g. exact date, estimated amount) — empty array if
              nothing's missing.
            - cleanedDescription stays in the customer's own words as much as
              possible.
            - Do not decide whether this is a valid claim or estimate whether
              it will be approved.
            """;

    private final AnthropicClient anthropicClient;

    @PostMapping("/extract")
    public ClaimAssistantResponse extract(@Valid @RequestBody ClaimAssistantRequest req) {
        List<ClaimAssistantRequest.PolicyOption> policies = req.getPolicies() == null ? List.of() : req.getPolicies();

        String policyList = policies.isEmpty()
                ? "The customer has no policies on file."
                : policies.stream()
                    .map(p -> "id=%s type=%s number=%s plan=%s".formatted(p.getId(), p.getType(), p.getPolicyNumber(), p.getPlanName()))
                    .collect(Collectors.joining("\n"));

        String userMessage = "Customer's policies:\n" + policyList
                + "\n\nCustomer's narrative:\n" + PromptGuard.wrapUntrusted(req.getNarrative());

        return anthropicClient.completeStructured(PromptGuard.ANTI_INJECTION_PREAMBLE + "\n" + SYSTEM_PROMPT, userMessage, ClaimDraft.class)
                .map(draft -> toResponse(draft, policies, req.getNarrative()))
                .orElseGet(() -> fallbackResponse(req.getNarrative()));
    }

    private ClaimAssistantResponse toResponse(ClaimDraft draft, List<ClaimAssistantRequest.PolicyOption> policies, String narrative) {
        // Server-side check on top of the prompt rule: a matched id must be one the customer actually owns.
        String matched = draft.matchedPolicyId();
        boolean known = matched != null && policies.stream().anyMatch(p -> matched.equals(p.getId()));
        String confidence = known ? (draft.matchConfidence() == null ? "low" : draft.matchConfidence()) : "none";
        List<String> questions = draft.clarifyingQuestions() == null ? List.of() : draft.clarifyingQuestions();
        String description = draft.cleanedDescription() == null || draft.cleanedDescription().isBlank()
                ? narrative : draft.cleanedDescription();
        return new ClaimAssistantResponse(
                known ? matched : null, confidence,
                draft.suggestedIncidentDate(), draft.suggestedClaimAmount(),
                description, questions, false);
    }

    private ClaimAssistantResponse fallbackResponse(String narrative) {
        return new ClaimAssistantResponse(
                null, "none", null, null, narrative,
                List.of("AI drafting assistant unavailable right now — please fill in the form fields directly."),
                true
        );
    }


}
