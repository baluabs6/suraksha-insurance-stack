package com.suraksha.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

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
