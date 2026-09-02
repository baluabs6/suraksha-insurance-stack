package com.suraksha.ai.claimassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suraksha.ai.client.AnthropicClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lets a customer describe an incident in plain English ("my car was hit in
 * the parking lot yesterday, looks like about 15k of damage to the bumper")
 * and turns it into a draft for the existing claim form fields, instead of
 * making them map their story onto claimType/incidentDate/claimAmount
 * themselves. Purely a drafting aid — the customer still reviews and edits
 * every field, and the actual POST /api/claims call is unchanged.
 */
@RestController
@RequestMapping("/api/ai/claim-assistant")
@RequiredArgsConstructor
@Slf4j
public class ClaimAssistantController {

    private static final String SYSTEM_PROMPT = """
            You turn a customer's plain-English description of an insurance
            incident into a draft claim form. You are given the customer's own
            policies (id, type, policy number, plan name) and their narrative.

            Respond with ONLY a single JSON object, no markdown fences, no
            preamble, matching exactly this shape:
            {
              "matchedPolicyId": "<one of the given policy ids, or null if none fit>",
              "matchConfidence": "high" | "low" | "none",
              "suggestedIncidentDate": "<YYYY-MM-DD or null if not mentioned>",
              "suggestedClaimAmount": "<plain number, no currency symbol or commas, or null if not mentioned>",
              "cleanedDescription": "<the narrative rewritten as a clear, neutral, factual 2-3 sentence claim description, in the customer's own words as much as possible, no invented details>",
              "clarifyingQuestions": ["<question>", ...]
            }

            Rules:
            - Never invent an incident date, amount, or detail the customer
              didn't mention. If something's missing, leave it null and add a
              clarifying question instead.
            - matchedPolicyId must be an id from the list given, or null — never
              invent one.
            - clarifyingQuestions should list only what's actually missing or
              ambiguous (e.g. exact date, estimated amount) — empty array if
              nothing's missing.
            - Do not decide whether this is a valid claim or estimate whether
              it will be approved.
            """;

    private final AnthropicClient anthropicClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/extract")
    public ClaimAssistantResponse extract(@Valid @RequestBody ClaimAssistantRequest req) {
        List<ClaimAssistantRequest.PolicyOption> policies = req.getPolicies() == null ? List.of() : req.getPolicies();

        String policyList = policies.isEmpty()
                ? "The customer has no policies on file."
                : policies.stream()
                    .map(p -> "id=%s type=%s number=%s plan=%s".formatted(p.getId(), p.getType(), p.getPolicyNumber(), p.getPlanName()))
                    .collect(Collectors.joining("\n"));

        String userMessage = "Customer's policies:\n" + policyList
                + "\n\nCustomer's narrative:\n" + req.getNarrative();

        String fallbackJson = "__FALLBACK__"; // sentinel, handled below rather than parsed as JSON
        String raw = anthropicClient.complete(SYSTEM_PROMPT, userMessage, fallbackJson);

        if (fallbackJson.equals(raw) || !anthropicClient.isConfigured()) {
            return fallbackResponse(req.getNarrative());
        }

        try {
            String cleaned = stripCodeFences(raw);
            JsonNode node = mapper.readTree(cleaned);
            List<String> questions = new ArrayList<>();
            if (node.path("clarifyingQuestions").isArray()) {
                node.path("clarifyingQuestions").forEach(q -> questions.add(q.asText()));
            }
            return new ClaimAssistantResponse(
                    nullableText(node, "matchedPolicyId"),
                    node.path("matchConfidence").asText("none"),
                    nullableText(node, "suggestedIncidentDate"),
                    nullableText(node, "suggestedClaimAmount"),
                    node.path("cleanedDescription").asText(req.getNarrative()),
                    questions,
                    false
            );
        } catch (Exception e) {
            log.warn("Could not parse claim-assistant JSON response, falling back to raw narrative.", e);
            return fallbackResponse(req.getNarrative());
        }
    }

    private ClaimAssistantResponse fallbackResponse(String narrative) {
        return new ClaimAssistantResponse(
                null, "none", null, null, narrative,
                List.of("AI drafting assistant unavailable right now — please fill in the form fields directly."),
                true
        );
    }

    private static String nullableText(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return (v.isMissingNode() || v.isNull()) ? null : v.asText();
    }

    private static String stripCodeFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\n", "").replaceFirst("```\\s*$", "");
        }
        return t.trim();
    }
}
