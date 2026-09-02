package com.suraksha.ai.adjuster;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdjusterQueryRequest {
    @NotBlank(message = "Enter a question.")
    private String question;

    // Optional scoping — narrows retrieval before anything is handed to the
    // model. At least one should normally be set; if both are blank, the
    // 25 most recent claims across the book are used (demo-scale only).
    private String policyId;   // UUID string
    private String riskLevel;  // LOW / MEDIUM / HIGH, matches FraudScoringService's output
}
