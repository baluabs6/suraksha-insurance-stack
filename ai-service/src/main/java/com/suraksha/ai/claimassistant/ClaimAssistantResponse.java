package com.suraksha.ai.claimassistant;

import java.util.List;

/**
 * Everything here is a *draft suggestion* for prefilling the claim form —
 * the frontend must still show it to the customer as editable fields before
 * submitting anything to POST /api/claims. This service never files a claim
 * itself.
 */
public record ClaimAssistantResponse(
        String matchedPolicyId,      // best-guess policy id from the list given, or null
        String matchConfidence,      // "high" | "low" | "none"
        String suggestedIncidentDate,  // ISO yyyy-MM-dd, or null if not mentioned
        String suggestedClaimAmount,   // plain number as string, or null if not mentioned
        String cleanedDescription,     // narrative rewritten as a clear, factual claim description
        List<String> clarifyingQuestions, // things the assistant couldn't determine and should ask about
        boolean usedFallback
) {
}
