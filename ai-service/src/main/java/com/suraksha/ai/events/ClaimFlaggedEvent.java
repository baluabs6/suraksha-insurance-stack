package com.suraksha.ai.events;

import java.util.List;
import java.util.UUID;

public record ClaimFlaggedEvent(
        UUID claimId,
        double riskScore,
        String riskLevel,
        List<String> flags,
        String modelVersion
) {
    public static final String TOPIC = "claim.flagged";
}
