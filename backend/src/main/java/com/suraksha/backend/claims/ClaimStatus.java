package com.suraksha.backend.claims;

public enum ClaimStatus {
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    SETTLED;

    /** Allowed adjuster transitions. SETTLED is terminal; REJECTED can be reopened for appeal. */
    public boolean canTransitionTo(ClaimStatus next) {
        return switch (this) {
            case SUBMITTED -> next == UNDER_REVIEW || next == APPROVED || next == REJECTED;
            case UNDER_REVIEW -> next == APPROVED || next == REJECTED;
            case APPROVED -> next == SETTLED;
            case REJECTED -> next == UNDER_REVIEW;
            case SETTLED -> false;
        };
    }
}
