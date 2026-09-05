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

    // Encrypted at the application layer with AES-256-GCM before it ever
    // reaches the database — see EncryptedStringConverter. A raw DB dump or
    // unauthorized read of a replica does not expose these in plaintext.
    @Convert(converter = EncryptedStringConverter.class)
    @Column
    private String panNumber;

    @Convert(converter = EncryptedStringConverter.class)
    @Column
    private String aadhaarNumber;

    // TOTP shared secret — also encrypted at rest, since anyone who reads it
    // in plaintext can generate valid MFA codes for this account.
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
