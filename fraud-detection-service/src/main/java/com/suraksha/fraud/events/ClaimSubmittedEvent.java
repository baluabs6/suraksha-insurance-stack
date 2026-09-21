package com.suraksha.fraud.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
