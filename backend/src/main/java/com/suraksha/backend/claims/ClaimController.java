package com.suraksha.backend.claims;

import com.suraksha.backend.claims.dto.ClaimRequest;
import com.suraksha.backend.claims.events.ClaimEventPublisher;
import com.suraksha.backend.health.CoverageService;
import com.suraksha.backend.health.HealthClaimAssessor;
import com.suraksha.backend.health.HealthClaimValidator;
import com.suraksha.backend.health.PolicyMember;
import com.suraksha.backend.health.PolicyMemberRepository;
import com.suraksha.backend.policy.Policy;
import com.suraksha.backend.policy.PolicyRepository;
import com.suraksha.backend.policy.PolicyStatus;
import com.suraksha.backend.policy.PolicyType;
import com.suraksha.backend.product.ClaimDetailsValidator;
import com.suraksha.backend.product.ProductRegistry;
import com.suraksha.backend.user.User;
import com.suraksha.backend.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final ClaimEventPublisher claimEventPublisher;
    private final ClaimStatusHistoryRepository claimStatusHistoryRepository;
    private final PolicyMemberRepository policyMemberRepository;
    private final CoverageService coverageService;

    @GetMapping
    public List<Claim> myClaims(Authentication auth) {
        return claimRepository.findByUserIdOrderBySubmittedAtDesc(UUID.fromString(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<?> fileClaim(@Valid @RequestBody ClaimRequest req, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());

        Policy policy = policyRepository.findById(req.getPolicyId())
                .filter(p -> p.getUser().getId().equals(userId))
                .orElse(null);
        if (policy == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Policy not found for this account."));
        }

        LocalDate today = LocalDate.now();
        boolean health = policy.getType() == PolicyType.HEALTH;
        // For a health claim the "incident" is the hospital admission.
        LocalDate incidentDate = health && req.getAdmissionDate() != null ? req.getAdmissionDate() : req.getIncidentDate();

        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(Map.of("message", "This policy is not active, so a claim can't be filed against it."));
        }
        if (incidentDate.isAfter(today)) {
            return ResponseEntity.badRequest().body(Map.of("message", "The incident date can't be in the future."));
        }
        if (incidentDate.isBefore(policy.getStartDate()) || incidentDate.isAfter(policy.getEndDate())) {
            return ResponseEntity.badRequest().body(Map.of("message", "The incident date falls outside this policy's coverage period."));
        }
        if (req.getClaimAmount().compareTo(policy.getCoverageAmount()) > 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "The claim amount is more than this policy's coverage."));
        }

        BigDecimal eligibleAmount = null;
        String assessmentNotes = null;
        PolicyMember member = null;
        if (health) {
            String error = HealthClaimValidator.validate(req, policy, today);
            if (error != null) {
                return ResponseEntity.badRequest().body(Map.of("message", error));
            }
            member = policyMemberRepository.findByIdAndPolicyId(req.getMemberId(), policy.getId()).orElse(null);
            if (member == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "The selected member isn't insured under this policy."));
            }
            var coverage = coverageService.summarize(policy);
            if (coverage.remaining().signum() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "The sum insured on this policy has been fully used or reserved by other claims."));
            }
            HealthClaimAssessor.Result assessment = HealthClaimAssessor.assess(new HealthClaimAssessor.Input(
                    new HealthClaimAssessor.Bill(req.getRoomCharges(), req.getProcedureCharges(), req.getMedicineCharges(),
                            req.getDiagnosticCharges(), req.getOtherCharges()),
                    req.getAdmissionDate(), req.getDischargeDate(), Boolean.TRUE.equals(req.getAccidental()),
                    req.getDiagnosis(), member.getPreExistingConditions(), policy.getStartDate(),
                    coverage.roomRentCapPerDay(), coverage.coPayPercent(), coverage.initialWaitingDays(),
                    coverage.preExistingWaitingMonths(), coverage.remaining()));
            eligibleAmount = assessment.eligibleAmount();
            assessmentNotes = assessment.toNotes();
        }

        // Every non-health line: the type-specific details are checked against that product's field definitions.
        Map<String, Object> details = null;
        if (!health) {
            var checked = ClaimDetailsValidator.validate(ProductRegistry.get(policy.getType()), req.getDetails());
            if (checked.error() != null) {
                return ResponseEntity.badRequest().body(Map.of("message", checked.error()));
            }
            details = checked.details();
        }

        User user = userRepository.findById(userId).orElseThrow();

        Claim claim = Claim.builder()
                .policy(policy)
                .user(user)
                .claimType(policy.getType().name())
                .claimAmount(req.getClaimAmount())
                .incidentDate(incidentDate)
                .description(req.getDescription())
                .status(ClaimStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .details(details)
                .build();
        if (health) {
            claim.setMemberId(member.getId());
            claim.setHospitalName(req.getHospitalName().trim());
            claim.setAdmissionDate(req.getAdmissionDate());
            claim.setDischargeDate(req.getDischargeDate());
            claim.setDiagnosis(req.getDiagnosis().trim());
            claim.setTreatingDoctor(req.getTreatingDoctor());
            claim.setAccidental(Boolean.TRUE.equals(req.getAccidental()));
            claim.setRoomCharges(req.getRoomCharges());
            claim.setProcedureCharges(req.getProcedureCharges());
            claim.setMedicineCharges(req.getMedicineCharges());
            claim.setDiagnosticCharges(req.getDiagnosticCharges());
            claim.setOtherCharges(req.getOtherCharges());
            claim.setEligibleAmount(eligibleAmount);
            claim.setAssessmentNotes(assessmentNotes);
        }

        claimRepository.save(claim);
        claimStatusHistoryRepository.save(ClaimStatusHistory.builder()
                .claimId(claim.getId())
                .fromStatus(null)
                .toStatus(ClaimStatus.SUBMITTED)
                .changedByUserId(null)
                .note(null)
                .build());
        claimEventPublisher.publishSubmitted(claim);
        return ResponseEntity.status(201).body(claim);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return claimRepository.findById(id)
                .filter(c -> c.getUser().getId().equals(userId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Claim not found for this account.")));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> history(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        boolean owned = claimRepository.findById(id).map(c -> c.getUser().getId().equals(userId)).orElse(false);
        if (!owned) {
            return ResponseEntity.status(404).body(Map.of("message", "Claim not found for this account."));
        }
        return ResponseEntity.ok(claimStatusHistoryRepository.findByClaimIdOrderByOccurredAtAsc(id));
    }
}
