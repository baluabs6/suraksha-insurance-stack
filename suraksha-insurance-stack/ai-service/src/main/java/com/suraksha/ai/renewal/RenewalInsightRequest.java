package com.suraksha.ai.renewal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RenewalInsightRequest {
    @NotBlank(message = "policyId is required.")
    private String policyId;

    private String planName;
    private String type;               // HEALTH / MOTOR / LIFE
    private BigDecimal coverageAmount;
    private BigDecimal premium;
    private LocalDate endDate;

    // Claims-history counts on this policy — the frontend already has this
    // from GET /api/claims, filtered client-side to this policy, same
    // client-supplied-context pattern as chat/recommendations.
    private int totalClaimsFiled;
    private int highRiskClaims;
    private int mediumRiskClaims;
}
