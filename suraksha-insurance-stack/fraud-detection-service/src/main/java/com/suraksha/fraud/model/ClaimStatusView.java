package com.suraksha.fraud.model;

// Mirrors com.suraksha.backend.claims.ClaimStatus — this service only ever
// reads and compares against these values, never introduces new ones.
public enum ClaimStatusView {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    SETTLED
}
