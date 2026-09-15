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

/**
 * Once a day, finds ACTIVE policies renewing within the next 30 days and
 * raises a one-time in-app reminder for each (guarded by
 * NotificationRepository.existsByUserIdAndTypeAndRelatedEntityId so a policy
 * doesn't get re-notified every day it stays inside that window).
 *
 * Runs at a fixed delay from application startup rather than a cron
 * expression tied to wall-clock time, so it also fires shortly after boot in
 * local/demo environments instead of only at 2 AM.
 */
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
