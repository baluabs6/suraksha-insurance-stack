package com.suraksha.ai.urgency;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UrgencyRequest {
    @NotBlank(message = "Provide the text to classify.")
    private String text;

    private String claimType;
}
