package com.suraksha.backend.config;

import com.suraksha.backend.claims.Claim;
import com.suraksha.backend.claims.ClaimRepository;
import com.suraksha.backend.claims.ClaimStatus;
import com.suraksha.backend.claims.ClaimStatusHistory;
import com.suraksha.backend.claims.ClaimStatusHistoryRepository;
import com.suraksha.backend.payment.Payment;
import com.suraksha.backend.payment.PaymentRepository;
import com.suraksha.backend.payment.PaymentStatus;
import com.suraksha.backend.policy.Policy;
import com.suraksha.backend.policy.PolicyRepository;
import com.suraksha.backend.policy.PolicyStatus;
import com.suraksha.backend.policy.PolicyType;
import com.suraksha.backend.user.Role;
import com.suraksha.backend.user.User;
import com.suraksha.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final ClaimStatusHistoryRepository claimStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("demo@suraksha.in")) {
            return;
        }

        User demoUser = userRepository.save(User.builder()
                .fullName("Ananya Rao")
                .email("demo@suraksha.in")
                .passwordHash(passwordEncoder.encode("Demo@1234"))
                .role(Role.CUSTOMER)
                .kycVerified(true)
                .build());

        User demoAgent = userRepository.save(User.builder()
                .fullName("Rahul Menon")
                .email("agent@suraksha.in")
                .passwordHash(passwordEncoder.encode("Demo@1234"))
                .role(Role.AGENT)
                .kycVerified(true)
                .build());

        userRepository.save(User.builder()
                .fullName("Priya Iyer")
                .email("adjuster@suraksha.in")
                .passwordHash(passwordEncoder.encode("Demo@1234"))
                .role(Role.CLAIMS_ADJUSTER)
                .kycVerified(true)
                .build());

        userRepository.save(User.builder()
                .fullName("Suresh Nair")
                .email("admin@suraksha.in")
                .passwordHash(passwordEncoder.encode("Demo@1234"))
                .role(Role.ADMIN)
                .kycVerified(true)
                .build());

        Policy health = policyRepository.save(Policy.builder()
                .user(demoUser).agent(demoAgent).policyNumber("POL-HL-2201394").planName("CarePlus Family")
                .type(PolicyType.HEALTH).coverageAmount(new BigDecimal("1000000")).premium(new BigDecimal("18500"))
                .startDate(LocalDate.of(2026, 1, 4)).endDate(LocalDate.of(2027, 1, 3))
                .status(PolicyStatus.ACTIVE).build());

        Policy motor = policyRepository.save(Policy.builder()
                .user(demoUser).agent(demoAgent).policyNumber("POL-MT-4590213").planName("DriveSecure Comprehensive")
                .type(PolicyType.MOTOR).coverageAmount(new BigDecimal("800000")).premium(new BigDecimal("9200"))
                .startDate(LocalDate.of(2026, 2, 18)).endDate(LocalDate.of(2027, 2, 17))
                .status(PolicyStatus.ACTIVE).build());

        policyRepository.save(Policy.builder()
                .user(demoUser).policyNumber("POL-LF-1102938").planName("LifeShield Term 30")
                .type(PolicyType.LIFE).coverageAmount(new BigDecimal("5000000")).premium(new BigDecimal("24000"))
                .startDate(LocalDate.of(2026, 1, 10)).endDate(LocalDate.of(2056, 1, 9))
                .status(PolicyStatus.ACTIVE).build());

        Claim healthClaim = claimRepository.save(Claim.builder()
                .policy(health).user(demoUser).claimType("HEALTH")
                .claimAmount(new BigDecimal("45000")).incidentDate(LocalDate.of(2026, 8, 20))
                .description("Hospitalization for a viral fever, three-day admission.")
                .status(ClaimStatus.UNDER_REVIEW).submittedAt(Instant.parse("2026-08-20T10:00:00Z"))
                .build());
        claimStatusHistoryRepository.save(ClaimStatusHistory.builder()
                .claimId(healthClaim.getId()).fromStatus(null).toStatus(ClaimStatus.SUBMITTED)
                .occurredAt(Instant.parse("2026-08-20T10:00:00Z")).build());
        claimStatusHistoryRepository.save(ClaimStatusHistory.builder()
                .claimId(healthClaim.getId()).fromStatus(ClaimStatus.SUBMITTED).toStatus(ClaimStatus.UNDER_REVIEW)
                .occurredAt(Instant.parse("2026-08-21T11:00:00Z")).build());

        Claim motorClaim = claimRepository.save(Claim.builder()
                .policy(motor).user(demoUser).claimType("MOTOR")
                .claimAmount(new BigDecimal("12000")).incidentDate(LocalDate.of(2026, 7, 1))
                .description("Rear bumper damage from a parking-lot collision.")
                .status(ClaimStatus.SETTLED).submittedAt(Instant.parse("2026-07-02T09:00:00Z"))
                .build());
        claimStatusHistoryRepository.save(ClaimStatusHistory.builder()
                .claimId(motorClaim.getId()).fromStatus(null).toStatus(ClaimStatus.SUBMITTED)
                .occurredAt(Instant.parse("2026-07-02T09:00:00Z")).build());
        claimStatusHistoryRepository.save(ClaimStatusHistory.builder()
                .claimId(motorClaim.getId()).fromStatus(ClaimStatus.SUBMITTED).toStatus(ClaimStatus.APPROVED)
                .occurredAt(Instant.parse("2026-07-05T09:00:00Z")).build());
        claimStatusHistoryRepository.save(ClaimStatusHistory.builder()
                .claimId(motorClaim.getId()).fromStatus(ClaimStatus.APPROVED).toStatus(ClaimStatus.SETTLED)
                .occurredAt(Instant.parse("2026-07-10T09:00:00Z")).build());

        paymentRepository.save(Payment.builder()
                .policy(health).amount(new BigDecimal("18500")).method("UPI")
                .gatewayTxnId("MOCK-DEMO0001").status(PaymentStatus.PAID)
                .paidAt(Instant.parse("2026-08-04T08:00:00Z")).build());

        paymentRepository.save(Payment.builder()
                .policy(motor).amount(new BigDecimal("9200")).method("Card")
                .gatewayTxnId("MOCK-DEMO0002").status(PaymentStatus.PAID)
                .paidAt(Instant.parse("2026-05-18T08:00:00Z")).build());
    }
}
