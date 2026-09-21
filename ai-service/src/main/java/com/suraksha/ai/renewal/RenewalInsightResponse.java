package com.suraksha.ai.renewal;

import java.util.List;

public record RenewalInsightResponse(
        String daysToRenewalNote,
        List<String> factors,
        String explanation,
        boolean usedFallback
) {
}
