package com.suraksha.backend.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KnownDeviceRepository extends JpaRepository<KnownDevice, UUID> {
    Optional<KnownDevice> findByUserIdAndDeviceFingerprint(UUID userId, String deviceFingerprint);
}
