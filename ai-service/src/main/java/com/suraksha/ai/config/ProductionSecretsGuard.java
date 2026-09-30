package com.suraksha.ai.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Refuses to start under the "prod" profile if a secret is still a committed development default. */
@Component
@Profile("prod")
public class ProductionSecretsGuard {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.internal-service-token}")
    private String internalToken;

    @PostConstruct
    void validate() {
        List<String> problems = new ArrayList<>();
        if (looksLikeDevDefault(jwtSecret) || jwtSecret.length() < 32) {
            problems.add("JWT_SECRET is a development default or shorter than 32 characters");
        }
        if (looksLikeDevDefault(internalToken) || internalToken.length() < 32) {
            problems.add("INTERNAL_SERVICE_TOKEN is a development default or shorter than 32 characters");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Refusing to start in prod: " + String.join("; ", problems));
        }
    }

    static boolean looksLikeDevDefault(String value) {
        if (value == null) return true;
        String v = value.toLowerCase();
        return v.contains("dev-only") || v.contains("change-me") || v.contains("change-this") || v.contains("replace_me");
    }
}
