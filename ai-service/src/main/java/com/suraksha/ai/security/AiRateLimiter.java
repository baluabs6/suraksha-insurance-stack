package com.suraksha.ai.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caps how many model-backed chat requests one signed-in user can make per minute and per day, so a single
 * account (or a stolen session) can't run up the AI bill. Counters live in memory, so with several replicas each
 * replica enforces its own limit; move them to Redis if you need one shared limit.
 */
@Component
public class AiRateLimiter {

    private record Window(long minute, int minuteCount, LocalDate day, int dayCount) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int perMinute;
    private final int perDay;

    public AiRateLimiter(@Value("${ai.limits.requests-per-minute:20}") int perMinute,
                         @Value("${ai.limits.requests-per-day:200}") int perDay) {
        this(Clock.systemUTC(), perMinute, perDay);
    }

    AiRateLimiter(Clock clock, int perMinute, int perDay) {
        this.clock = clock;
        this.perMinute = perMinute;
        this.perDay = perDay;
    }

    /** Counts one request for the user; returns false (and does not count it) when a limit is already reached. */
    public boolean tryAcquire(String userId) {
        long minute = clock.millis() / 60_000;
        LocalDate day = LocalDate.now(clock);
        boolean[] allowed = {false};

        windows.compute(userId, (k, w) -> {
            int minuteCount = w != null && w.minute() == minute ? w.minuteCount() : 0;
            int dayCount = w != null && day.equals(w.day()) ? w.dayCount() : 0;
            if (minuteCount >= perMinute || dayCount >= perDay) {
                allowed[0] = false;
                return new Window(minute, minuteCount, day, dayCount);
            }
            allowed[0] = true;
            return new Window(minute, minuteCount + 1, day, dayCount + 1);
        });
        return allowed[0];
    }
}
