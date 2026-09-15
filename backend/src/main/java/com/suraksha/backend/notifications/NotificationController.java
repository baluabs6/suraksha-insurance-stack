package com.suraksha.backend.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public List<Notification> mine(Authentication auth) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(auth.getName()));
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        return Map.of("count", notificationRepository.countByUserIdAndReadFalse(UUID.fromString(auth.getName())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return notificationRepository.findById(id)
                .filter(n -> n.getUserId().equals(userId))
                .map(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                    return ResponseEntity.ok(n);
                })
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Notification not found for this account.")));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().filter(n -> !n.isRead()).toList();
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return ResponseEntity.ok(Map.of("updated", unread.size()));
    }
}
