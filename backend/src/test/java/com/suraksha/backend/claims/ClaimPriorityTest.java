package com.suraksha.backend.claims;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ClaimPriorityTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    @Test
    void higherRiskRanksAboveLowerRiskAtTheSameAge() {
        Instant submitted = NOW.minusSeconds(3600 * 5);
        assertTrue(ClaimPriority.score(0.9, submitted, true, NOW).priority()
                > ClaimPriority.score(0.1, submitted, true, NOW).priority());
    }

    @Test
    void olderClaimsRankAboveNewerOnesAndBreachTheSla() {
        var fresh = ClaimPriority.score(0.2, NOW.minusSeconds(3600), true, NOW);
        var old = ClaimPriority.score(0.2, NOW.minusSeconds(3600 * 60), true, NOW);
        assertTrue(old.priority() > fresh.priority());
        assertTrue(old.slaBreached());
        assertFalse(fresh.slaBreached());
    }

    @Test
    void unassignedClaimsGetABoostAndMissingRiskCountsAsZero() {
        Instant submitted = NOW.minusSeconds(3600 * 10);
        assertTrue(ClaimPriority.score(0.5, submitted, false, NOW).priority()
                > ClaimPriority.score(0.5, submitted, true, NOW).priority());
        assertEquals(ClaimPriority.score(0.0, submitted, true, NOW).priority(),
                ClaimPriority.score(null, submitted, true, NOW).priority());
    }
}
