package com.suraksha.backend.security;

import com.suraksha.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JwtService {

    private static final String MFA_PENDING_PURPOSE = "mfa_pending";

    private final Key key;
    private final long accessMinutes;
    private final long refreshDays;
    private final StringRedisTemplate redisTemplate;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-minutes}") long accessMinutes,
            @Value("${app.jwt.refresh-token-days}") long refreshDays,
            StringRedisTemplate redisTemplate) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMinutes = accessMinutes;
        this.refreshDays = refreshDays;
        this.redisTemplate = redisTemplate;
    }


    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(accessMinutes, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }


    public String generateMfaPendingToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("purpose", MFA_PENDING_PURPOSE)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public UUID validateMfaPendingToken(String token) {
        Claims claims = parseAccessToken(token);
        if (claims == null || !MFA_PENDING_PURPOSE.equals(claims.get("purpose", String.class))) {
            return null;
        }
        return UUID.fromString(claims.getSubject());
    }


    public String generateAndStoreRefreshToken(UUID userId) {
        String familyId = UUID.randomUUID().toString();
        return issueRefreshToken(userId, familyId);
    }

    public RefreshOutcome rotateRefreshToken(String presentedToken) {
        String presentedHash = hash(presentedToken);
        String mapping = redisTemplate.opsForValue().get("refresh:" + presentedHash);
        if (mapping == null) {
            return RefreshOutcome.invalid();
        }

        String[] parts = mapping.split(":", 2);
        UUID userId = UUID.fromString(parts[0]);
        String familyId = parts[1];

        String currentHashForFamily = redisTemplate.opsForValue().get("refresh-family:" + familyId);
        if (currentHashForFamily == null || !currentHashForFamily.equals(presentedHash)) {
            revokeFamily(familyId);
            return RefreshOutcome.reuseDetected(userId);
        }

        redisTemplate.delete("refresh:" + presentedHash);
        String newToken = issueRefreshToken(userId, familyId);
        return RefreshOutcome.rotated(userId, newToken);
    }

    public void revokeRefreshToken(String token) {
        String tokenHash = hash(token);
        String mapping = redisTemplate.opsForValue().get("refresh:" + tokenHash);
        if (mapping != null) {
            revokeFamily(mapping.split(":", 2)[1]);
        } else {
            redisTemplate.delete("refresh:" + tokenHash);
        }
    }

    private String issueRefreshToken(UUID userId, String familyId) {
        String token = UUID.randomUUID().toString();
        String tokenHash = hash(token);
        redisTemplate.opsForValue().set("refresh:" + tokenHash, userId + ":" + familyId, refreshDays, TimeUnit.DAYS);
        redisTemplate.opsForValue().set("refresh-family:" + familyId, tokenHash, refreshDays, TimeUnit.DAYS);
        return token;
    }

    private void revokeFamily(String familyId) {
        String hash = redisTemplate.opsForValue().get("refresh-family:" + familyId);
        if (hash != null) {
            redisTemplate.delete("refresh:" + hash);
        }
        redisTemplate.delete("refresh-family:" + familyId);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
