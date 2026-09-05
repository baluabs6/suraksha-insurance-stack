package com.suraksha.ai.events;

import java.util.List;
import java.util.UUID;

// Mirrors com.suraksha.fraud.events.ClaimFlaggedEvent — see the note in that
// file about why this is a deliberate copy, not a shared library.
public record ClaimFlaggedEvent(
        UUID claimId,
        double riskScore,
        String riskLevel,
        List<String> flags,
        String modelVersion
) {
    public static final String TOPIC = "claim.flagged";
}
