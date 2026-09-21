package com.suraksha.ai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Service
public class JwtValidationService {

    private final Key key;

    public JwtValidationService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims validate(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            if (claims.get("purpose") != null) {
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
