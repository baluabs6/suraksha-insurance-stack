package com.suraksha.fraud.listener;

import com.suraksha.fraud.events.ClaimFlaggedEvent;
import com.suraksha.fraud.events.ClaimSubmittedEvent;
import com.suraksha.fraud.model.ClaimRecord;
import com.suraksha.fraud.repository.ClaimRecordRepository;
import com.suraksha.fraud.service.FraudScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimSubmittedListener {

    private final FraudScoringService fraudScoringService;
    private final ClaimRecordRepository claimRecordRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${fraud.model-version}")
    private String modelVersion;

    @KafkaListener(topics = ClaimSubmittedEvent.TOPIC, groupId = "fraud-detection-service")
    public void onClaimSubmitted(ClaimSubmittedEvent event) {
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
            kafkaTemplate.send(ClaimFlaggedEvent.TOPIC, event.claimId().toString(), flaggedEvent);
            log.info("Claim {} flagged at level {} ({})", event.claimId(), result.riskLevel(), result.flags());
        }
    }
}
