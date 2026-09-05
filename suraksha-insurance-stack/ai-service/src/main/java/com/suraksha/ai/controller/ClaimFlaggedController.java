package com.suraksha.ai.controller;

import com.suraksha.ai.events.ClaimFlaggedEvent;
import com.suraksha.ai.triage.ClaimTriageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Replaces the old @KafkaListener(topics = ClaimFlaggedEvent.TOPIC) —
// fraud-detection-service now calls this endpoint directly instead of
// publishing to Kafka. See the auth note in fraud-detection-service's
// ClaimSubmittedController — the same caveat applies here.
@RestController
@RequiredArgsConstructor
public class ClaimFlaggedController {

    private final ClaimTriageService claimTriageService;

    @PostMapping("/internal/claims/flagged")
    public ResponseEntity<Void> onClaimFlagged(@RequestBody ClaimFlaggedEvent event) {
        claimTriageService.handleClaimFlagged(event);
        return ResponseEntity.accepted().build();
    }
}
