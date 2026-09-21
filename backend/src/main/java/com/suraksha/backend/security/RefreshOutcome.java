package com.suraksha.backend.security;

import java.util.UUID;

public record RefreshOutcome(Status status, UUID userId, String newToken) {

    public enum Status { ROTATED, INVALID, REUSE_DETECTED }

    public static RefreshOutcome rotated(UUID userId, String newToken) {
        return new RefreshOutcome(Status.ROTATED, userId, newToken);
    }

    public static RefreshOutcome invalid() {
        return new RefreshOutcome(Status.INVALID, null, null);
    }

    public static RefreshOutcome reuseDetected(UUID userId) {
        return new RefreshOutcome(Status.REUSE_DETECTED, userId, null);
    }
}
