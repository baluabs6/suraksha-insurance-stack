package com.suraksha.fraud.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// Mirrors com.suraksha.backend.claims.events.ClaimSubmittedEvent field-for-field.
// Kept as a separate copy deliberately: these are two independently deployable
// services, and a shared library would couple their release cycles together.
// If the event shape changes, treat it as an API contract change (version the
// topic or the schema) rather than editing both copies in lockstep silently.
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
