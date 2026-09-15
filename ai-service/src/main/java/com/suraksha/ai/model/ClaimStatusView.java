package com.suraksha.ai.model;

// Mirrors com.suraksha.backend.claims.ClaimStatus — this service only ever
// reads this value, never introduces new statuses or transitions a claim.
public enum ClaimStatusView {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    SETTLED
}
