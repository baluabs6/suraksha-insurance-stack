package com.suraksha.backend.claims.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Published to the "claim.submitted" Kafka topic right after a claim is saved.
// The fraud-detection service consumes this to score the claim asynchronously —
// the user gets an immediate "submitted" response without waiting on scoring.
public record ClaimSubmittedEvent(
        UUID claimId,
        UUID policyId,
        UUID userId,
        String claimType,
        BigDecimal claimAmount,
        LocalDate incidentDate
) {
    public static final String TOPIC = "claim.submitted";
}
