package com.suraksha.backend.claims;

import org.junit.jupiter.api.Test;

import static com.suraksha.backend.claims.ClaimStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class ClaimStatusTest {

    @Test
    void settledIsTerminal() {
        for (ClaimStatus next : ClaimStatus.values()) {
            assertFalse(SETTLED.canTransitionTo(next));
        }
    }

    @Test
    void cannotGoBackToSubmitted() {
        for (ClaimStatus from : ClaimStatus.values()) {
            assertFalse(from.canTransitionTo(SUBMITTED));
        }
    }

    @Test
    void normalPathIsAllowed() {
        assertTrue(SUBMITTED.canTransitionTo(UNDER_REVIEW));
        assertTrue(UNDER_REVIEW.canTransitionTo(APPROVED));
        assertTrue(APPROVED.canTransitionTo(SETTLED));
        assertTrue(REJECTED.canTransitionTo(UNDER_REVIEW));
    }
}
