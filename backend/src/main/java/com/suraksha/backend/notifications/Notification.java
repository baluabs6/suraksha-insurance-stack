package com.suraksha.backend.notifications;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * In-app notification shown to a customer (claim status changes, payment
 * confirmations, renewal reminders). Deliberately in-app-only for this demo
 * — see NotificationService for where an email/SMS provider would plug in.
 */
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

    // e.g. CLAIM_STATUS_CHANGED, CLAIM_FLAGGED, PAYMENT_CONFIRMED, PAYMENT_FAILED,
    // RENEWAL_REMINDER, GRIEVANCE_UPDATED
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    // Free-form reference (claim id, policy id, payment id, grievance id) so the
    // frontend can deep-link the notification to the relevant record.
    private UUID relatedEntityId;

    @Builder.Default
    @Column(nullable = false)
    private boolean read = false;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
