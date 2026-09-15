package com.suraksha.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Progressive account lockout, independent of the IP-based RateLimitService.
 * IP-based limits stop a single script hammering the endpoint; this stops
 * credential stuffing distributed across many IPs against one account,
 * which IP-based limiting alone can't catch.
 *
 * Deliberately time-boxed (not a permanent lock): a permanent lock triggered
 * by a stranger entering a wrong password becomes its own denial-of-service
 * vector against the real account owner.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_FAILURES = 8;
    private static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public boolean isLocked(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey(email)));
    }

    public void recordFailure(String email) {
        String key = failKey(email);
        Long failures = redisTemplate.opsForValue().increment(key);
        if (failures != null && failures == 1L) {
            redisTemplate.expire(key, LOCKOUT_WINDOW);
        }
        if (failures != null && failures >= MAX_FAILURES) {
            redisTemplate.opsForValue().set(lockKey(email), "1", LOCKOUT_WINDOW);
        }
    }

    public void recordSuccess(String email) {
        redisTemplate.delete(failKey(email));
        redisTemplate.delete(lockKey(email));
    }

    private String failKey(String email) {
        return "login-fail:" + email.toLowerCase();
    }

    private String lockKey(String email) {
        return "login-lock:" + email.toLowerCase();
    }
}
