package com.suraksha.backend.security;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "known_devices", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "deviceFingerprint"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnownDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String deviceFingerprint;

    @Column(nullable = false)
    private Instant firstSeenAt;

    @Column(nullable = false)
    private Instant lastSeenAt;
}
