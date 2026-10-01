package com.suraksha.ai.limits;

import com.suraksha.ai.security.AiRateLimiter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class AiRateLimiterTest {

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-01T10:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advanceSeconds(long s) { now = now.plusSeconds(s); }
    }

    private static AiRateLimiter limiter(Clock clock, int perMinute, int perDay) throws Exception {
        Constructor<AiRateLimiter> c = AiRateLimiter.class.getDeclaredConstructor(Clock.class, int.class, int.class);
        c.setAccessible(true);
        return c.newInstance(clock, perMinute, perDay);
    }

    @Test
    void blocksOnceTheMinuteLimitIsReachedAndRecoversNextMinute() throws Exception {
        MutableClock clock = new MutableClock();
        AiRateLimiter limiter = limiter(clock, 2, 100);
        assertTrue(limiter.tryAcquire("u1"));
        assertTrue(limiter.tryAcquire("u1"));
        assertFalse(limiter.tryAcquire("u1"));
        clock.advanceSeconds(61);
        assertTrue(limiter.tryAcquire("u1"));
    }

    @Test
    void usersAreCountedSeparately() throws Exception {
        AiRateLimiter limiter = limiter(new MutableClock(), 1, 100);
        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("b"));
    }

    @Test
    void dailyLimitHoldsAcrossMinutesAndResetsTheNextDay() throws Exception {
        MutableClock clock = new MutableClock();
        AiRateLimiter limiter = limiter(clock, 100, 3);
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.tryAcquire("u"));
            clock.advanceSeconds(120);
        }
        assertFalse(limiter.tryAcquire("u"));
        clock.advanceSeconds(24 * 3600);
        assertTrue(limiter.tryAcquire("u"));
    }
}
