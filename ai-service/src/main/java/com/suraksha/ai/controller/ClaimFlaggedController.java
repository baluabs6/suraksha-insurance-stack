package com.suraksha.ai.controller;

import com.suraksha.ai.events.ClaimFlaggedEvent;
import com.suraksha.ai.triage.ClaimTriageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
