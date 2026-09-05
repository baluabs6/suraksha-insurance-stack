package com.suraksha.backend.security;

import java.util.UUID;

/**
 * Result of presenting a refresh token to be rotated.
 *
 * REUSE_DETECTED means the presented token was once valid for this session
 * but is no longer the current one — i.e. it was already rotated out. That
 * only happens if someone (attacker or the legitimate device racing a retry)
 * is replaying an old token, so the whole session family gets revoked rather
 * than trusted.
 */
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
