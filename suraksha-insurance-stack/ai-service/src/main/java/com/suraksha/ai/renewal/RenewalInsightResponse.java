package com.suraksha.ai.renewal;

import java.util.List;

public record RenewalInsightResponse(
        String daysToRenewalNote,   // deterministic, rule-based
        List<String> factors,       // deterministic, rule-based — what's actually driving this
        String explanation,         // LLM narrative written on top of the factors above
        boolean usedFallback
) {
}
