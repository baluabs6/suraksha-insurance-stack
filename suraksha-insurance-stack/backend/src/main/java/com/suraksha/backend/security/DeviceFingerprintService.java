package com.suraksha.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Lightweight "new device" detection: fingerprints User-Agent + the /24 of
 * the client IP (coarse on purpose — full IPs change too often on mobile
 * networks to be a stable fingerprint, and we don't want to store raw IPs
 * indefinitely anyway). Not meant to be a strong device identifier on its
 * own — it's a low-cost trigger for "this looks like a new device, log it
 * and consider a step-up challenge," not a security boundary by itself.
 */
@Service
@RequiredArgsConstructor
public class DeviceFingerprintService {

    private final KnownDeviceRepository knownDeviceRepository;

    public String fingerprint(String userAgent, String ipAddress) {
        String coarseIp = coarsen(ipAddress);
        String raw = (userAgent == null ? "unknown-ua" : userAgent) + "|" + coarseIp;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] out = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** @return true if this is the first time this fingerprint has been seen for this user (i.e. a new device). */
    public boolean recordAndCheckIfNew(UUID userId, String fingerprint) {
        var existing = knownDeviceRepository.findByUserIdAndDeviceFingerprint(userId, fingerprint);
        Instant now = Instant.now();
        if (existing.isPresent()) {
            KnownDevice device = existing.get();
            device.setLastSeenAt(now);
            knownDeviceRepository.save(device);
            return false;
        }
        knownDeviceRepository.save(KnownDevice.builder()
                .userId(userId)
                .deviceFingerprint(fingerprint)
                .firstSeenAt(now)
                .lastSeenAt(now)
                .build());
        return true;
    }

    private String coarsen(String ipAddress) {
        if (ipAddress == null) return "unknown";
        int lastDot = ipAddress.lastIndexOf('.');
        return lastDot > 0 ? ipAddress.substring(0, lastDot) + ".0" : ipAddress;
    }
}
