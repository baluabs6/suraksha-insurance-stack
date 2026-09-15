package com.suraksha.ai.policycompare;

import com.suraksha.ai.model.PolicyTypeView;

import java.util.List;

public record PolicyComparisonResponse(
        List<CandidatePlan> candidatePlans,
        String comparison,
        boolean usedFallback
) {
    public record CandidatePlan(PolicyTypeView type, String planName, String covers) {
    }
}
