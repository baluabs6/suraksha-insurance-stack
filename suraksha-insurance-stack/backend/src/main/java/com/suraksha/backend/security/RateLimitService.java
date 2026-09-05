package com.suraksha.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed fixed-window rate limiter. Not as smooth as a sliding-window
 * or token-bucket algorithm, but it needs no new dependency, is easy to
 * reason about, and is enough to stop credential-stuffing / scripted brute
 * force from hammering auth endpoints at scale.
 *
 * Fixed-window has a known edge case (a burst can land across a window
 * boundary and briefly allow ~2x the limit) — acceptable here because the
 * real defense against credential stuffing is the per-account lockout in
 * {@link LoginAttemptService}; this is the first, cheap line of defense.
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    /**
     * @return true if the caller is still within the allowed limit and the
     *         request should proceed; false if the limit is exceeded and the
     *         request should be rejected (HTTP 429).
     */
    public boolean allow(String bucketKey, int maxAttempts, Duration window) {
        String key = "ratelimit:" + bucketKey;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count == null || count <= maxAttempts;
    }
}
