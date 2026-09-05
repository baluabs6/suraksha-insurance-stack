package com.suraksha.fraud.controller;

import com.suraksha.fraud.client.AiServiceClient;
import com.suraksha.fraud.events.ClaimFlaggedEvent;
import com.suraksha.fraud.events.ClaimSubmittedEvent;
import com.suraksha.fraud.model.ClaimRecord;
import com.suraksha.fraud.repository.ClaimRecordRepository;
import com.suraksha.fraud.service.FraudScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Replaces the old @KafkaListener(topics = ClaimSubmittedEvent.TOPIC) —
// backend now calls this endpoint directly right after saving a claim,
// instead of publishing to Kafka. Scoring logic in FraudScoringService is
// completely unchanged.
//
// Note for production: this endpoint isn't authenticated. In the original
// Kafka setup, only something with broker credentials could publish a
// claim.submitted event; now, anything that can reach this service on the
// network can trigger a (re-)score. Put this behind network-level
// restrictions (a private service / internal-only networking) or add
// service-to-service auth before this goes further than a demo.
@RestController
@RequiredArgsConstructor
@Slf4j
public class ClaimSubmittedController {

    private final FraudScoringService fraudScoringService;
    private final ClaimRecordRepository claimRecordRepository;
    private final AiServiceClient aiServiceClient;

    @Value("${fraud.model-version}")
    private String modelVersion;

    @PostMapping("/internal/claims/submitted")
    public ResponseEntity<Void> onClaimSubmitted(@RequestBody ClaimSubmittedEvent event) {
        log.info("Scoring claim {}", event.claimId());

        var result = fraudScoringService.score(event);

        claimRecordRepository.findById(event.claimId()).ifPresentOrElse(
                (ClaimRecord claim) -> {
                    claim.setRiskScore(result.riskScore());
                    claim.setRiskLevel(result.riskLevel());
                    claimRecordRepository.save(claim);
                },
                () -> log.warn("Claim {} not found when trying to record risk score — it may not have "
                        + "committed to the database yet; consider a retry/backoff policy here in production.",
                        event.claimId())
        );

        if (!result.flags().isEmpty()) {
            ClaimFlaggedEvent flaggedEvent = new ClaimFlaggedEvent(
                    event.claimId(), result.riskScore(), result.riskLevel(), result.flags(), modelVersion);
            aiServiceClient.notifyClaimFlagged(flaggedEvent);
            log.info("Claim {} flagged at level {} ({})", event.claimId(), result.riskLevel(), result.flags());
        }

        return ResponseEntity.accepted().build();
    }
}
