package com.suraksha.ai.claimassistant;

import java.util.List;

public record ClaimAssistantResponse(
        String matchedPolicyId,
        String matchConfidence,
        String suggestedIncidentDate,
        String suggestedClaimAmount,
        String cleanedDescription,
        List<String> clarifyingQuestions,
        boolean usedFallback
) {
}
