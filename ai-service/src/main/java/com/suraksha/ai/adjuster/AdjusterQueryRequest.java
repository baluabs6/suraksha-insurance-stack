package com.suraksha.ai.adjuster;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdjusterQueryRequest {
    @NotBlank(message = "Enter a question.")
    private String question;

    private String policyId;
    private String riskLevel;
}
