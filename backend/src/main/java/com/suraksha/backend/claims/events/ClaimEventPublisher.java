package com.suraksha.backend.claims.events;

import com.suraksha.backend.claims.Claim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSubmitted(Claim claim) {
        ClaimSubmittedEvent event = new ClaimSubmittedEvent(
                claim.getId(),
                claim.getPolicy().getId(),
                claim.getUser().getId(),
                claim.getClaimType(),
                claim.getClaimAmount(),
                claim.getIncidentDate()
        );
        // Key by claimId so any future partitioned consumer group processes
        // all events for a given claim in order.
        kafkaTemplate.send(ClaimSubmittedEvent.TOPIC, claim.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // Don't fail the user's claim submission if Kafka is down —
                        // log it; the claim still exists and can be scored later
                        // by a backfill job. Never let async scoring block the request.
                        log.error("Failed to publish claim.submitted for claim {}", claim.getId(), ex);
                    }
                });
    }
}
