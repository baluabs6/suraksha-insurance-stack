package com.suraksha.backend.grievance;

import com.suraksha.backend.audit.AuditService;
import com.suraksha.backend.grievance.dto.GrievanceRequest;
import com.suraksha.backend.grievance.dto.GrievanceResolutionRequest;
import com.suraksha.backend.notifications.NotificationService;
import com.suraksha.backend.user.User;
import com.suraksha.backend.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal IRDAI-style grievance-redressal flow: customers file a complaint
 * and get a reference number they can quote; adjusters/admins triage and
 * resolve it. See Grievance.java for what's simplified vs. a real
 * integration with IRDAI's Integrated Grievance Management System.
 */
@RestController
@RequiredArgsConstructor
public class GrievanceController {

    private final GrievanceRepository grievanceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    @PostMapping("/api/grievances")
    public ResponseEntity<?> file(@Valid @RequestBody GrievanceRequest req, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        User user = userRepository.findById(userId).orElseThrow();

        Grievance grievance = Grievance.builder()
                .referenceNumber(generateReferenceNumber())
                .user(user)
                .category(req.getCategory())
                .description(req.getDescription())
                .relatedClaimId(req.getRelatedClaimId())
                .relatedPolicyId(req.getRelatedPolicyId())
                .status(GrievanceStatus.OPEN)
                .build();
        grievanceRepository.save(grievance);

        auditService.record(userId, "GRIEVANCE_FILED", null, null,
                "grievanceId=" + grievance.getId() + " ref=" + grievance.getReferenceNumber());

        return ResponseEntity.status(201).body(grievance);
    }

    @GetMapping("/api/grievances")
    public List<Grievance> mine(Authentication auth) {
        return grievanceRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(auth.getName()));
    }

    @GetMapping("/api/grievances/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return grievanceRepository.findById(id)
                .filter(g -> g.getUser().getId().equals(userId))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Grievance not found for this account.")));
    }

    @GetMapping("/api/adjuster/grievances")
    @PreAuthorize("hasAnyRole('CLAIMS_ADJUSTER', 'ADMIN')")
    public List<Grievance> queue(@RequestParam(required = false) GrievanceStatus status) {
        return status == null
                ? grievanceRepository.findAllByOrderByCreatedAtDesc()
                : grievanceRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @PatchMapping("/api/adjuster/grievances/{id}")
    @PreAuthorize("hasAnyRole('CLAIMS_ADJUSTER', 'ADMIN')")
    public ResponseEntity<?> resolve(@PathVariable UUID id, @Valid @RequestBody GrievanceResolutionRequest req,
                                      Authentication auth) {
        Grievance grievance = grievanceRepository.findById(id).orElse(null);
        if (grievance == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Grievance not found."));
        }

        UUID staffId = UUID.fromString(auth.getName());
        grievance.setStatus(req.getStatus());
        grievance.setResolutionNote(req.getNote());
        grievance.setResolvedByUserId(staffId);
        if (req.getStatus() == GrievanceStatus.RESOLVED || req.getStatus() == GrievanceStatus.CLOSED) {
            grievance.setResolvedAt(Instant.now());
        }
        grievanceRepository.save(grievance);

        notificationService.create(
                grievance.getUser().getId(),
                "GRIEVANCE_UPDATED",
                "Complaint " + grievance.getReferenceNumber() + " updated",
                "Your complaint is now " + req.getStatus().name().toLowerCase().replace('_', ' ') + ".",
                grievance.getId());

        auditService.record(staffId, "GRIEVANCE_RESOLVED", null, null,
                "grievanceId=" + grievance.getId() + " status=" + req.getStatus());

        return ResponseEntity.ok(grievance);
    }

    private String generateReferenceNumber() {
        int suffix = 100000 + random.nextInt(900000);
        return "GRV-" + Year.now().getValue() + "-" + suffix;
    }
}
