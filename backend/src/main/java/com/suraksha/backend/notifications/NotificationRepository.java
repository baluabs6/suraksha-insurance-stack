package com.suraksha.backend.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserIdAndReadFalse(UUID userId);
    boolean existsByUserIdAndTypeAndRelatedEntityId(UUID userId, String type, UUID relatedEntityId);
}
