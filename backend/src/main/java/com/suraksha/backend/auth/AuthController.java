package com.suraksha.backend.auth;

import com.suraksha.backend.auth.dto.LoginRequest;
import com.suraksha.backend.auth.dto.RegisterRequest;
import com.suraksha.backend.auth.dto.UserResponse;
import com.suraksha.backend.security.CookieUtil;
import com.suraksha.backend.security.JwtService;
import com.suraksha.backend.user.Role;
import com.suraksha.backend.user.User;
import com.suraksha.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.status(409).body(Map.of("message", "An account with this email already exists."));
        }
        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(Role.CUSTOMER)
                .kycVerified(false)
                .build();
        userRepository.save(user);
        return ResponseEntity.status(201).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect email or password."));
        }
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateAndStoreRefreshToken(user.getId());
        cookieUtil.addAuthCookies(response, accessToken, refreshToken);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = "refresh_token", required = false) String refreshToken,
                                      HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("message", "No refresh token present."));
        }
        UUID userId = jwtService.validateRefreshToken(refreshToken);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Refresh token is invalid or expired."));
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        cookieUtil.addAccessCookie(response, jwtService.generateAccessToken(user));
        return ResponseEntity.ok(Map.of("message", "Token refreshed."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refresh_token", required = false) String refreshToken,
                                     HttpServletResponse response) {
        if (refreshToken != null) {
            jwtService.revokeRefreshToken(refreshToken);
        }
        cookieUtil.clearAuthCookies(response);
        return ResponseEntity.ok(Map.of("message", "Logged out."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return userRepository.findById(userId)
                .map(u -> ResponseEntity.ok(UserResponse.from(u)))
                .orElse(ResponseEntity.status(401).build());
    }
}
