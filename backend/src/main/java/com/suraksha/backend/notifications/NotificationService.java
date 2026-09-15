package com.suraksha.backend.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Single entry point for raising a notification. In-app only for this demo —
 * bolting on email/SMS later means adding a NotificationDispatcher interface
 * here (SES/SNS/Twilio/etc.) and calling it from create() alongside the DB
 * write, without touching any of the ~10 call sites across the codebase.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification create(UUID userId, String type, String title, String message, UUID relatedEntityId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityId(relatedEntityId)
                .build();
        return notificationRepository.save(notification);
    }
}
