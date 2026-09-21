package com.suraksha.backend.notifications;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    private UUID relatedEntityId;

    @Builder.Default
    @Column(nullable = false)
    private boolean read = false;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
