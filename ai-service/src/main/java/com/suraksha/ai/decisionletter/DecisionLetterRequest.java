package com.suraksha.ai.decisionletter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DecisionLetterRequest {
    @NotNull(message = "claimId is required.")
    private UUID claimId;

    // The decision the adjuster already made — this endpoint drafts the
    // letter for it, it never makes or suggests the decision itself.
    @NotBlank(message = "decision is required.")
    private String decision; // APPROVED | REJECTED | SETTLED

    // Optional internal note the adjuster wrote when changing status —
    // used as grounding context (e.g. "missing invoice"), not quoted verbatim.
    private String internalNote;
}
