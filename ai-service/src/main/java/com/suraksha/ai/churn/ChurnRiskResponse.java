package com.suraksha.ai.churn;

import java.util.List;

public record ChurnRiskResponse(
        String churnRiskBand,   // LOW / MEDIUM / HIGH — deterministic, rule-based
        List<String> factors,   // deterministic, rule-based
        String narrative,       // LLM-written retention-outreach guidance on top of the factors
        boolean usedFallback
) {
}
