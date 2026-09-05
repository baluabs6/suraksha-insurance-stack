package com.suraksha.ai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Validates access tokens issued by backend's JwtService. ai-service shares
 * the same signing secret (APP_JWT_SECRET / app.jwt.secret) but never issues
 * tokens itself — verification only, which is why this is much smaller than
 * backend's JwtService.
 *
 * This is what was previously missing: without this, ai-service and every
 * endpoint under /api/ai/** had no authentication at all, as flagged in the
 * README and in AdjusterQueryController's own comments.
 */
@Service
public class JwtValidationService {

    private final Key key;

    public JwtValidationService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** @return the token's claims if valid and not an MFA-pending token, or null otherwise. */
    public Claims validate(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            if (claims.get("purpose") != null) {
                // MFA-pending tokens must never be usable as a real session token.
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
