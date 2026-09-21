package com.suraksha.ai.churn;

import java.util.List;

public record ChurnRiskResponse(
        String churnRiskBand,
        List<String> factors,
        String narrative,
        boolean usedFallback
) {
}
