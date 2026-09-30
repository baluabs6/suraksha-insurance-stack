package com.suraksha.ai.claimassistant;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Target type for Spring AI structured output: the model's reply is mapped straight onto this. */
public record ClaimDraft(
        @JsonPropertyDescription("Exactly one of the policy ids provided, or null if none clearly fits")
        String matchedPolicyId,
        @JsonPropertyDescription("How sure the policy match is: high, low or none")
        String matchConfidence,
        @JsonPropertyDescription("Incident date as YYYY-MM-DD, or null if the customer did not mention one")
        String suggestedIncidentDate,
        @JsonPropertyDescription("Claim amount as a plain number with no currency symbol or commas, or null if not mentioned")
        String suggestedClaimAmount,
        @JsonPropertyDescription("The narrative rewritten as a clear, neutral, factual 2-3 sentence description with no invented details")
        String cleanedDescription,
        @JsonPropertyDescription("Questions for anything missing or ambiguous; empty list if nothing is missing")
        List<String> clarifyingQuestions
) {
}
