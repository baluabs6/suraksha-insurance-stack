package com.suraksha.ai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates service-to-service calls under /internal/** using a shared
 * secret header, since these calls (fraud-detection-service -> ai-service)
 * have no end-user JWT to present. This was previously wide open — anyone
 * who could reach ai-service could POST a fabricated claim.flagged event.
 *
 * A shared secret is a pragmatic middle ground for a demo; mTLS between
 * services (see k8s/ Istio notes in SECURITY.md) is the stronger long-term
 * answer since it authenticates at the connection level and doesn't rely on
 * a static secret that every internal caller must keep.
 */
@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    @Value("${app.internal-service-token}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/internal/")) {
            String presented = request.getHeader("X-Internal-Service-Token");
            if (presented == null || !constantTimeEquals(presented, expectedToken)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid internal service token.");
                return;
            }
            var authentication = new UsernamePasswordAuthenticationToken(
                    "internal-service", null, List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }
}
