package com.suraksha.ai.urgency;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UrgencyRequest {
    @NotBlank(message = "Provide the text to classify.")
    private String text;

    // Optional — HEALTH/MOTOR/LIFE, helps calibrate what "urgent" looks like
    // for this claim type.
    private String claimType;
}
