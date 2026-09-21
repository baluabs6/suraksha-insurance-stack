package com.suraksha.ai.decisionletter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DecisionLetterRequest {
    @NotNull(message = "claimId is required.")
    private UUID claimId;

    @NotBlank(message = "decision is required.")
    private String decision;

    private String internalNote;
}
