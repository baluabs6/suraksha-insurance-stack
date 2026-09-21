package com.suraksha.backend.user;

import com.suraksha.backend.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Builder.Default
    @Column(nullable = false)
    private boolean kycVerified = false;

    @Convert(converter = EncryptedStringConverter.class)
    @Column
    private String panNumber;

    @Convert(converter = EncryptedStringConverter.class)
    @Column
    private String aadhaarNumber;

    @Convert(converter = EncryptedStringConverter.class)
    @Column
    private String mfaSecret;

    @Builder.Default
    @Column(nullable = false)
    private boolean mfaEnabled = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
