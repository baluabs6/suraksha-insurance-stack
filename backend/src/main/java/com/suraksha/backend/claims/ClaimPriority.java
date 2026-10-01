package com.suraksha.backend.claims;

import java.time.Duration;
import java.time.Instant;

/**
 * Ranks open claims for the adjuster workbench: higher risk, closer to (or past) the review deadline, and
 * unassigned claims float to the top. It only orders the queue; it never decides anything about a claim.
 */
public final class ClaimPriority {

    /** Target time to first decision. */
    public static final long SLA_HOURS = 48;

    public record Score(double priority, long hoursOpen, boolean slaBreached) {}

    private ClaimPriority() {}

    public static Score score(Double riskScore, Instant submittedAt, boolean assigned, Instant now) {
        long hoursOpen = Math.max(0, Duration.between(submittedAt, now).toHours());
        double risk = riskScore == null ? 0.0 : Math.max(0.0, Math.min(1.0, riskScore));
        double age = Math.min(1.5, (double) hoursOpen / SLA_HOURS);
        double priority = 50.0 * risk + 40.0 * age + (assigned ? 0.0 : 10.0);
        return new Score(Math.round(priority * 10.0) / 10.0, hoursOpen, hoursOpen >= SLA_HOURS);
    }
}
