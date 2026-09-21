package com.suraksha.backend.auth;

import com.suraksha.backend.audit.AuditService;
import com.suraksha.backend.auth.dto.*;
import com.suraksha.backend.security.*;
import com.suraksha.backend.user.Role;
import com.suraksha.backend.user.User;
import com.suraksha.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
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
    private final RateLimitService rateLimitService;
    private final LoginAttemptService loginAttemptService;
    private final TotpService totpService;
    private final AuditService auditService;
    private final DeviceFingerprintService deviceFingerprintService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        if (!rateLimitService.allow("register:ip:" + ip, 5, Duration.ofHours(1))) {
            return ResponseEntity.status(429).body(Map.of("message", "Too many registration attempts. Please try again later."));
        }
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
        auditService.record(user.getId(), "ACCOUNT_REGISTERED", ip, httpRequest.getHeader("User-Agent"), null);
        return ResponseEntity.status(201).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpRequest,
                                    HttpServletResponse response) {
        String ip = clientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String email = req.getEmail().toLowerCase();

        if (!rateLimitService.allow("login:ip:" + ip, 15, Duration.ofMinutes(15))) {
            return ResponseEntity.status(429).body(Map.of("message", "Too many login attempts from this network. Please try again later."));
        }
        if (loginAttemptService.isLocked(email)) {
            auditService.record(null, "LOGIN_LOCKED", ip, userAgent, "email=" + email);
            return ResponseEntity.status(429).body(Map.of("message",
                    "This account is temporarily locked after repeated failed attempts. Try again in a few minutes."));
        }

        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(email);
            auditService.record(user == null ? null : user.getId(), "LOGIN_FAILURE", ip, userAgent, "email=" + email);
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect email or password."));
        }

        loginAttemptService.recordSuccess(email);

        if (user.isMfaEnabled()) {
            String mfaToken = jwtService.generateMfaPendingToken(user.getId());
            auditService.record(user.getId(), "MFA_CHALLENGE_ISSUED", ip, userAgent, null);
            return ResponseEntity.ok(LoginResponse.challenge(mfaToken));
        }

        return ResponseEntity.ok(LoginResponse.success(
                completeLogin(user, httpRequest, response)));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@Valid @RequestBody MfaVerifyRequest req, HttpServletRequest httpRequest,
                                        HttpServletResponse response) {
        String ip = clientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        if (!rateLimitService.allow("mfa-verify:ip:" + ip, 15, Duration.ofMinutes(15))) {
            return ResponseEntity.status(429).body(Map.of("message", "Too many attempts. Please try again later."));
        }

        UUID userId = jwtService.validateMfaPendingToken(req.getMfaToken());
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "MFA challenge expired or invalid — please log in again."));
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isMfaEnabled() || user.getMfaSecret() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "MFA challenge expired or invalid — please log in again."));
        }
        if (!totpService.verifyCode(user.getMfaSecret(), req.getCode())) {
            auditService.record(userId, "MFA_CHALLENGE_FAILED", ip, userAgent, null);
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect code."));
        }

        auditService.record(userId, "MFA_CHALLENGE_SUCCESS", ip, userAgent, null);
        return ResponseEntity.ok(completeLogin(user, httpRequest, response));
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<?> setupMfa(Authentication authentication) {
        User user = currentUser(authentication);
        String secret = totpService.generateSecret();
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);
        userRepository.save(user);
        String otpAuthUrl = totpService.buildOtpAuthUrl(secret, user.getEmail(), "Suraksha Insurance");
        return ResponseEntity.ok(new MfaSetupResponse(secret, otpAuthUrl));
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<?> enableMfa(@Valid @RequestBody MfaCodeRequest req, Authentication authentication,
                                        HttpServletRequest httpRequest) {
        User user = currentUser(authentication);
        if (user.getMfaSecret() == null) {
            return ResponseEntity.status(400).body(Map.of("message", "Call /api/auth/mfa/setup first."));
        }
        if (!totpService.verifyCode(user.getMfaSecret(), req.getCode())) {
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect code."));
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
        auditService.record(user.getId(), "MFA_ENABLED", clientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);
        return ResponseEntity.ok(Map.of("message", "MFA enabled."));
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<?> disableMfa(@Valid @RequestBody LoginRequest req, Authentication authentication,
                                         HttpServletRequest httpRequest) {
        User user = currentUser(authentication);
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect password."));
        }
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        auditService.record(user.getId(), "MFA_DISABLED", clientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);
        return ResponseEntity.ok(Map.of("message", "MFA disabled."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = "refresh_token", required = false) String refreshToken,
                                      HttpServletRequest httpRequest, HttpServletResponse response) {
        String ip = clientIp(httpRequest);
        if (!rateLimitService.allow("refresh:ip:" + ip, 60, Duration.ofHours(1))) {
            return ResponseEntity.status(429).body(Map.of("message", "Too many requests. Please try again later."));
        }
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of("message", "No refresh token present."));
        }

        RefreshOutcome outcome = jwtService.rotateRefreshToken(refreshToken);
        switch (outcome.status()) {
            case REUSE_DETECTED -> {
                auditService.record(outcome.userId(), "REFRESH_REUSE_DETECTED", ip, httpRequest.getHeader("User-Agent"), null);
                cookieUtil.clearAuthCookies(response);
                return ResponseEntity.status(401).body(Map.of("message", "Session invalidated — please log in again."));
            }
            case INVALID -> {
                return ResponseEntity.status(401).body(Map.of("message", "Refresh token is invalid or expired."));
            }
            default -> {
                User user = userRepository.findById(outcome.userId()).orElse(null);
                if (user == null) {
                    return ResponseEntity.status(401).build();
                }
                cookieUtil.addAuthCookies(response, jwtService.generateAccessToken(user), outcome.newToken());
                return ResponseEntity.ok(Map.of("message", "Token refreshed."));
            }
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refresh_token", required = false) String refreshToken,
                                     Authentication authentication, HttpServletRequest httpRequest,
                                     HttpServletResponse response) {
        if (refreshToken != null) {
            jwtService.revokeRefreshToken(refreshToken);
        }
        cookieUtil.clearAuthCookies(response);
        UUID userId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        auditService.record(userId, "LOGOUT", clientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);
        return ResponseEntity.ok(Map.of("message", "Logged out."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        return ResponseEntity.ok(UserResponse.from(currentUser(authentication)));
    }


    private UserResponse completeLogin(User user, HttpServletRequest httpRequest, HttpServletResponse response) {
        String ip = clientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateAndStoreRefreshToken(user.getId());
        cookieUtil.addAuthCookies(response, accessToken, refreshToken);

        String fingerprint = deviceFingerprintService.fingerprint(userAgent, ip);
        boolean isNewDevice = deviceFingerprintService.recordAndCheckIfNew(user.getId(), fingerprint);
        auditService.record(user.getId(), isNewDevice ? "NEW_DEVICE_LOGIN" : "LOGIN_SUCCESS", ip, userAgent, null);

        return UserResponse.from(user);
    }

    private User currentUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
