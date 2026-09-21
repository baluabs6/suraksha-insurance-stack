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
    private String type;
    private BigDecimal coverageAmount;
    private BigDecimal premium;
    private LocalDate endDate;

    private int totalClaimsFiled;
    private int highRiskClaims;
    private int mediumRiskClaims;
}
