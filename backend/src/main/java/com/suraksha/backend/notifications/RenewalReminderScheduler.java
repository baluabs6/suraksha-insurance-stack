package com.suraksha.backend.notifications;

import com.suraksha.backend.policy.Policy;
import com.suraksha.backend.policy.PolicyRepository;
import com.suraksha.backend.policy.PolicyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RenewalReminderScheduler {

    private static final String NOTIFICATION_TYPE = "RENEWAL_REMINDER";
    private static final int REMINDER_WINDOW_DAYS = 30;

    private final PolicyRepository policyRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(initialDelay = 60_000, fixedDelay = 24L * 60 * 60 * 1000)
    public void sendRenewalReminders() {
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(REMINDER_WINDOW_DAYS);

        List<Policy> upcoming = policyRepository.findByEndDateBetweenAndStatus(today, windowEnd, PolicyStatus.ACTIVE);
        int sent = 0;
        for (Policy policy : upcoming) {
            boolean alreadyNotified = notificationRepository.existsByUserIdAndTypeAndRelatedEntityId(
                    policy.getUser().getId(), NOTIFICATION_TYPE, policy.getId());
            if (alreadyNotified) {
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(today, policy.getEndDate());
            notificationService.create(
                    policy.getUser().getId(),
                    NOTIFICATION_TYPE,
                    "Renewal coming up: " + policy.getPolicyNumber(),
                    "Your " + policy.getPlanName() + " policy renews in " + daysLeft + " day(s). "
                            + "Renew on time to keep your coverage active without a gap.",
                    policy.getId());
            sent++;
        }
        if (sent > 0) {
            log.info("Sent {} renewal reminder(s).", sent);
        }
    }
}
