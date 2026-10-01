package com.suraksha.backend.policy;

import com.suraksha.backend.audit.AuditService;
import com.suraksha.backend.notifications.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Turns a bought-but-unpaid policy into an ACTIVE one once its first payment has cleared. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyActivationService {

    private final PolicyRepository policyRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    /** No-op (returns false) unless the policy is PENDING_PAYMENT, so renewal payments are unaffected. */
    @Transactional
    public boolean activateIfPending(UUID policyId) {
        Policy policy = policyRepository.findById(policyId).orElse(null);
        if (policy == null || policy.getStatus() != PolicyStatus.PENDING_PAYMENT) {
            return false;
        }

        // Paid after the requested start date: cover starts today and the term keeps its full length.
        LocalDate today = LocalDate.now();
        if (policy.getStartDate().isBefore(today)) {
            long shift = ChronoUnit.DAYS.between(policy.getStartDate(), today);
            policy.setStartDate(today);
            policy.setEndDate(policy.getEndDate().plusDays(shift));
        }
        policy.setStatus(PolicyStatus.ACTIVE);
        policyRepository.save(policy);

        UUID userId = policy.getUser().getId();
        notificationService.create(userId, "POLICY_ACTIVATED", "Your policy is active",
                policy.getPlanName() + " (" + policy.getPolicyNumber() + ") is active from "
                        + policy.getStartDate() + " until " + policy.getEndDate() + ".",
                policy.getId());
        auditService.record(userId, "POLICY_ACTIVATED", null, null, "policyId=" + policy.getId());
        log.info("Policy {} activated after payment.", policy.getPolicyNumber());
        return true;
    }
}
