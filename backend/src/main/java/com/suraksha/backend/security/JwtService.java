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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class JwtService {

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

    public String generateAndStoreRefreshToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("refresh:" + token, userId.toString(), refreshDays, TimeUnit.DAYS);
        return token;
    }

    public UUID validateRefreshToken(String token) {
        String userId = redisTemplate.opsForValue().get("refresh:" + token);
        return userId == null ? null : UUID.fromString(userId);
    }

    public void revokeRefreshToken(String token) {
        redisTemplate.delete("refresh:" + token);
    }
}
