package com.suraksha.backend.policy;

public enum PolicyStatus {
    /** Bought but not yet paid for. Claims can't be filed until the payment clears and the policy turns ACTIVE. */
    PENDING_PAYMENT,
    ACTIVE,
    EXPIRED,
    CANCELLED
}
