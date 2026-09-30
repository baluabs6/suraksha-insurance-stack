package com.suraksha.backend.security;

import com.suraksha.backend.user.Role;
import com.suraksha.backend.user.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-test-secret-test-secret-32chars", 15, 7, Mockito.mock(StringRedisTemplate.class));

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("a@b.in").role(Role.CUSTOMER).build();
    }

    @Test
    void mfaPendingTokenIsNotAcceptedAsAnAccessToken() {
        String pending = jwtService.generateMfaPendingToken(UUID.randomUUID());
        assertNull(jwtService.parseAccessToken(pending),
                "a password-only MFA challenge token must never authenticate a session");
    }

    @Test
    void mfaPendingTokenStillValidatesForTheMfaStep() {
        UUID id = UUID.randomUUID();
        assertEquals(id, jwtService.validateMfaPendingToken(jwtService.generateMfaPendingToken(id)));
    }

    @Test
    void accessTokenIsNotAcceptedAsMfaPendingToken() {
        assertNull(jwtService.validateMfaPendingToken(jwtService.generateAccessToken(user())));
    }

    @Test
    void genuineAccessTokenParses() {
        Claims claims = jwtService.parseAccessToken(jwtService.generateAccessToken(user()));
        assertNotNull(claims);
        assertEquals("CUSTOMER", claims.get("role", String.class));
    }
}
