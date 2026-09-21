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
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/internal/**").hasRole("INTERNAL_SERVICE")
                .requestMatchers("/api/ai/adjuster/**").hasAnyRole("CLAIMS_ADJUSTER", "ADMIN")
                .requestMatchers("/api/ai/fraud-explanation/**").hasAnyRole("CLAIMS_ADJUSTER", "ADMIN")
                .requestMatchers("/api/ai/decision-letter/**").hasAnyRole("CLAIMS_ADJUSTER", "ADMIN")
                .requestMatchers("/api/ai/churn-risk/**").hasAnyRole("AGENT", "CLAIMS_ADJUSTER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(internalServiceAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
