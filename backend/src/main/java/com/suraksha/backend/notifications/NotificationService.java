package com.suraksha.backend.notifications;

import com.suraksha.backend.user.UserRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates the in-app notification and, when enabled, sends the same text by e-mail.
 * E-mail is best effort and runs off the request thread, so a slow or failing mail server never delays a claim
 * status change or a payment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.notifications.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.notifications.email.from:no-reply@suraksha.in}")
    private String fromAddress;

    private final ExecutorService emailExecutor = Executors.newFixedThreadPool(2);

    public Notification create(UUID userId, String type, String title, String message, UUID relatedEntityId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityId(relatedEntityId)
                .build();
        Notification saved = notificationRepository.save(notification);
        sendEmailCopy(userId, title, message);
        return saved;
    }

    private void sendEmailCopy(UUID userId, String title, String message) {
        JavaMailSender sender = emailEnabled ? mailSender.getIfAvailable() : null;
        if (sender == null) return;

        emailExecutor.submit(() -> {
            try {
                userRepository.findById(userId).ifPresent(user -> {
                    SimpleMailMessage mail = new SimpleMailMessage();
                    mail.setFrom(fromAddress);
                    mail.setTo(user.getEmail());
                    mail.setSubject("Suraksha: " + title);
                    mail.setText(message + "\n\nSign in to Suraksha to see the details.");
                    sender.send(mail);
                });
            } catch (Exception e) {
                log.warn("Couldn't send the e-mail copy of a notification.", e);
            }
        });
    }

    @PreDestroy
    void shutdown() {
        emailExecutor.shutdown();
    }
}
