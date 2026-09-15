package com.suraksha.ai.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Puts every /api/ai/** endpoint behind the same JWT the main backend
 * issues. Previously this service had no auth at all — anyone who could
 * reach port 8082 could call the chatbot, recommendations, document
 * analysis, claim assistant, renewal insight, and adjuster query endpoints
 * directly. CSRF is left to the main backend (this service is only ever
 * called with a bearer/cookie token, not browser-form-style state changes),
 * but every request now requires a valid, non-expired access token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalServiceAuthFilter internalServiceAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // stateless bearer/cookie auth only, no browser session state to protect here
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/internal/**").hasRole("INTERNAL_SERVICE")
                .requestMatchers("/api/ai/adjuster/**").hasAnyRole("CLAIMS_ADJUSTER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(internalServiceAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
